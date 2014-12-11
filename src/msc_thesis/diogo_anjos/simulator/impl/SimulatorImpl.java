package msc_thesis.diogo_anjos.simulator.impl;

/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import msc_thesis.diogo_anjos.simulator.EnergyMeasureTupleDTO;
import msc_thesis.diogo_anjos.simulator.EnergyMeter;
import msc_thesis.diogo_anjos.simulator.Simulator;
import msc_thesis.diogo_anjos.simulator.SimulatorClient;

public class SimulatorImpl implements Simulator {

	private Connection database = null;
	private String meterDatabaseTable = null;
	private TimestampIndexPair tsIndexPair = null;
	private List<SimulatorClient> clientsLits = new ArrayList<SimulatorClient>();
	private volatile int speedTimeFactor = 1;
	private String initialSimulationTS = null;
	private String finalSimulationTS = null;
	
	private boolean alreadyStarted = false;
	private EnergyMeter meter = null;
	
	RoadRunner rr = null;
	
	
	public SimulatorImpl(EnergyMeter em) {
		setupDB(em);
		tsIndexPair = getInitialMeasureTimestamp();
	}
	
	public SimulatorImpl(EnergyMeter em, String initialTS, String finalTS) throws Exception {
		setupDB(em);
		TimestampIndexPair validatedPair = validateInputTimestamps(initialTS, finalTS);
		initialSimulationTS = validatedPair.getFirstTS();
		finalSimulationTS = validatedPair.getSecondTS();
		tsIndexPair = getInitialMeasureTimestamp(initialSimulationTS);
	}
	
	private void setupDB(EnergyMeter em){
		meter = em;
		database = connectToDB("localhost", "5432", "lumina_db", "postgres", "root");
		meterDatabaseTable = meter.getDatabaseTable();
	}
	

	public void start() {
		if (!alreadyStarted) {
			notifyClientsThatSimulationHasStarted();
			rr = new RoadRunner(meter);
			rr.start(); //Start Thread
			alreadyStarted = true;
		}
		else{
			rr.resumeRunner();
		}
	}

	public void stop() { 
		try{
			rr.pauseRunner();
		}catch(NullPointerException e){
			System.out.println("[ERROR]: Simulator is not yet started!");
		}
	}

	private class RoadRunner extends Thread {
		
		private volatile boolean keepWalking = true;
		private long simulationStartTime = 0;
		private String simulationFirstTS = null;
		private String simulationLastTS = null;
		private EnergyMeter simulatedMeter;
		
		public RoadRunner(EnergyMeter em){
			simulatedMeter = em;
		}
		
		
		@Override
		public void run() {
			long delta = 0;
			simulationStartTime = System.currentTimeMillis();
			while (true) {
				checkPauseWaitCondition(); // to avoid active waiting when sim. is paused 
				EnergyMeasureTupleDTO currentIndexDTO = queryDatastreamTupleByTimestamp(tsIndexPair.getFirstTS());
				pushDatastreamToClients(currentIndexDTO);
				ifInitialTimestampThenRegist(currentIndexDTO);
				delta = getDeltaBetweenTuples(tsIndexPair.getFirstTS(), tsIndexPair.getSecondTS());
				if(checkStopCondition(delta)){
					return;
				}
				tsIndexPair = queryTwoNextTimestamps(tsIndexPair.getFirstTS());
				try {
					Thread.sleep(delta/speedTimeFactor);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		private boolean checkStopCondition(long delta){
			// if pairs 2nd element == null, meaning that 1st is the last database record
			if(delta == -1 || (tsIndexPair.getFirstTS().equals(finalSimulationTS))) { 
				simulationLastTS = tsIndexPair.getFirstTS();
				String duration = milisecondsTo_HH_MM_SS_format(System.currentTimeMillis() - simulationStartTime);
				System.out.println("["+simulatedMeter+"]"+" Simulation completed! From "+simulationFirstTS+" to "+simulationLastTS+" in "+duration+"ms");
				notifyClientsThatSimulationHasFinished();
				return true;
			}
			return false;
		}
		
		private void ifInitialTimestampThenRegist(EnergyMeasureTupleDTO tuple){
			if(simulationFirstTS == null){ //first record
				simulationFirstTS = tuple.getMeasureTS();
			}
		}
		
		
		public synchronized void resumeRunner(){
			keepWalking = true;
			notifyAll();
		}
		
		public void pauseRunner(){
			keepWalking = false;
		}
		
		private synchronized void checkPauseWaitCondition(){
			while (!keepWalking) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}// RoadRunner class EOF

	public boolean setSpeedTimeFactor(int newFactor) {
		if (newFactor > 0) {
			speedTimeFactor = newFactor;
			return true;
		} else {
			return false;
		}
	}

	public int getSpeedTimeFactor() {
		return speedTimeFactor;
	}

	private EnergyMeasureTupleDTO queryDatastreamTupleByTimestamp(String targetTS) {
		String queryStatement = "SELECT * " + 
								"FROM " + meterDatabaseTable + 
								"WHERE measure_timestamp = " + "\'" + targetTS + "\'" + 
								"LIMIT 1"; // just
		try {
			return buildDtoFromResultSet(executeQuery(queryStatement));
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private EnergyMeasureTupleDTO buildDtoFromResultSet(ResultSet rs) {
		EnergyMeasureTupleDTO resDTO = null;
		try {
			if (rs.next()) {
				String measure_ts = rs.getString(1);
				String location = rs.getString(2);
				resDTO = new EnergyMeasureTupleDTO(measure_ts, location);
				resDTO.setPh1Ampere(rs.getString(3));
				resDTO.setPh1PowerFactor(rs.getString(4));
				resDTO.setPh1Volt(rs.getString(5));
				resDTO.setPh2Ampere(rs.getString(6));
				resDTO.setPh2PowerFactor(rs.getString(7));
				resDTO.setPh2Volt(rs.getString(8));
				resDTO.setPh3Ampere(rs.getString(9));
				resDTO.setPh3PowerFactor(rs.getString(10));
				resDTO.setPh3Volt(rs.getString(11));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
		return resDTO;
	}

	private long getDeltaBetweenTuples(String ts1, String ts2) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date ts1Date = null;
		Date ts2Date = null;
		try {
			ts1Date = sdf.parse(ts1);
		} catch (ParseException e) {
			System.out.println("[Error]: There is something wrong with First Index timestamp.");
			e.printStackTrace();
		}
		try {
			ts2Date = sdf.parse(ts2);
		} catch (ParseException | NullPointerException e) {
			return -1;
		}
		return (ts2Date.getTime() - ts1Date.getTime());
	}

	private TimestampIndexPair queryTwoNextTimestamps(String pivotTimestamp) {
		String queryStatement = "SELECT measure_timestamp " + 
								"FROM " + meterDatabaseTable + 
								"WHERE measure_timestamp > " + "\'" + pivotTimestamp + "\'" +
								" ORDER BY measure_timestamp ASC " + "LIMIT 2";
		return executeQueryAndBuildResultPair(queryStatement);
	}

	private TimestampIndexPair getInitialMeasureTimestamp() {
		String queryStatement = "SELECT measure_timestamp " + 
								"FROM " + meterDatabaseTable + 
								"ORDER BY measure_timestamp ASC " + 
								"LIMIT 2";
		return executeQueryAndBuildResultPair(queryStatement);
	}
	
	private TimestampIndexPair getInitialMeasureTimestamp(String pivotTimestamp) {
		String queryStatement = "SELECT measure_timestamp " + 
								"FROM " + meterDatabaseTable + 
								"WHERE measure_timestamp >= " + "\'" + pivotTimestamp + "\'" +
								"ORDER BY measure_timestamp ASC " + 
								"LIMIT 2";
		return executeQueryAndBuildResultPair(queryStatement);
	}

	/* Receives a TS as input and returns the very next TS in ascending order.
	 * If the input TS exists in the database, then this same TS will be the returned TS.
	 * This method serves mainly to validate the TS values passed by the client.
	 * If TS belongs to data, return this same TS, otherwise returns the very next TS belonging 
	 * to the database.
	 */
	private TimestampIndexPair getNearestMeasureTimestamp(String tsToValidate, TimestampIndexPair result){
		String queryStatement = "SELECT measure_timestamp " + 
								"FROM " + meterDatabaseTable + 
								"WHERE measure_timestamp >= " + "\'" + tsToValidate + "\'" +
								" ORDER BY measure_timestamp ASC " + 
								"LIMIT 1";
		try {
			ResultSet rs = executeQuery(queryStatement);
			while (rs.next()) {
				result.addTS(rs.getString(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	
	private TimestampIndexPair executeQueryAndBuildResultPair(String queryStatement) {
		try {
			ResultSet rs = executeQuery(queryStatement);
			TimestampIndexPair resTS = new TimestampIndexPair();
			while (rs.next()) {
				resTS.addTS(rs.getString(1));
			}
			return resTS;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	private ResultSet executeQuery(String queryStatement) throws SQLException {
		ResultSet rs = null;
		Statement st = database.createStatement();
		rs = st.executeQuery(queryStatement);
		return rs;
	}

	private Connection connectToDB(String hostname, String port, String dbname, String username, String password) {
		System.out.print("Connecting to DB: " + hostname + ":" + port + "/" + dbname + " with user/pass=" + username + "/" + password + "... ");
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("\n[ERROR]: Cannot find Simulator's JDBC driver.");
			// e.printStackTrace();
			return null;
		}
		try {
			String database_URI = "jdbc:postgresql://" + hostname + ":" + port + "/" + dbname;
			Connection db_connection = DriverManager.getConnection(database_URI, username, password);
			System.out.println("Done!");
			return db_connection;
		} catch (SQLException e) {
			System.out.println("\n[ERROR]: Cannot connect to Simulator's Database.");
			// e.printStackTrace();
			return null;
		}
	}

	public void destroy() {
		try {
			database.close();
			System.out.println("Simulator Destroyed! All resources were released.");
		} catch (SQLException | NullPointerException e) {
			System.out.println("[ERROR]: Cannot close Simulator's Database connection.");
			// e.printStackTrace();
		}
	}
	
	private class TimestampIndexPair {

		private String firstTS = null;
		private String secondTS = null;

		public boolean addTS(String ts) {
			if (firstTS == null) {
				firstTS = ts;
				return true;
			} else {
				if (secondTS == null) {
					secondTS = ts;
					return true;
				}
			}
			return false;
		}

		public String getFirstTS() {
			return firstTS;
		}

		public String getSecondTS() {
			return secondTS;
		}

		@Override
		public String toString() {
			return "<TS1=" + firstTS + ", TS2=" + secondTS + ">";

		}
	}

	private void pushDatastreamToClients(EnergyMeasureTupleDTO tuple) {
		for (SimulatorClient sc : clientsLits) {
			sc.receiveDatastream(tuple);
		}
	}
	
	
	private void notifyClientsThatSimulationHasStarted() {
		// so they the clients may know how many meters has started
		// so they may know for how many meters they should wait so 
		// they can properly assume that the simulation has finished
		for (SimulatorClient sc : clientsLits) {
			sc.simulationHasStartedNotification(this.meter);
		}
	}
	
	private void notifyClientsThatSimulationHasFinished() {
		// so they do not continue in the waiting state for more tuples
		for (SimulatorClient sc : clientsLits) {
			sc.simulationHasFinishedNotification(this.meter);
		}
	}

	@Override
	public void registerNewClient(SimulatorClient client) {
		clientsLits.add(client);

	}

	private String milisecondsTo_HH_MM_SS_format(long miliseconds) {
		long seconds = miliseconds / 1000;
		long ms = miliseconds % 1000;
		long s = seconds % 60;
		long m = (seconds / 60) % 60;
		long h = (seconds / (60 * 60));
		return String.format("%d:%02d:%02d:%03d", h, m, s, ms);
	}
	
	private TimestampIndexPair validateInputTimestamps(String initialTS, String finalTS) {
		TimestampIndexPair resPair = new TimestampIndexPair();
		if(getDeltaBetweenTuples(finalTS, initialTS) > 0){
			throw new IllegalArgumentException("[Error]: Params. initialTS > finalTS. This does not make sense!");
		}
		getNearestMeasureTimestamp(initialTS, resPair);
		if(resPair.getFirstTS()==null){
			throw new IllegalArgumentException("[Error]: Does not exist a TS >= "+initialTS+" in the Database.");
		}
		getNearestMeasureTimestamp(finalTS, resPair);
		if(resPair.getSecondTS()==null){
			throw new IllegalArgumentException("[Error]: Does not exist a TS >= "+finalTS+"  in the Database.");
		}
		return resPair;
	} 

	@Override
	public String toString() {
		if(initialSimulationTS != null && finalSimulationTS != null){
			return 	"Database table: " + meterDatabaseTable + ", simulation boundaries: "
					+ initialSimulationTS + " to "+finalSimulationTS;
		}
		return 	"Database table: " + meterDatabaseTable + ", simulation boundaries: all database";
	}
	
	/* This method is not necessary for now.
	private String incrementSecondsToDate(String date, long seconds) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date initialDate;
		try {
			initialDate = sdf.parse(date);
			Date incrementedDate = new Date(initialDate.getTime() + TimeUnit.SECONDS.toMillis(seconds));
			return sdf.format(incrementedDate.getTime());
		} catch (ParseException e) {
			System.out.println("[Error]: There is a problem with input's date format");
			e.printStackTrace();
			return null;
		}
	}*/
	
}// SimulatorImpl class EOF
