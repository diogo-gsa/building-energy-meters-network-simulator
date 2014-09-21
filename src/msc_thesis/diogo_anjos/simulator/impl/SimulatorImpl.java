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
import java.sql.Time;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

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
		meter = em;
	}
	
	public SimulatorImpl(EnergyMeter em, String initialTS, String finalTS) throws Exception {
		meter = em;
		TimestampIndexPair validatedPair = validateInputTimestamps(initialTS, finalTS);
		initialSimulationTS = validatedPair.getFirstTS();
		finalSimulationTS = validatedPair.getSecondTS();
		System.out.println("DEBUG Test: "+ initialSimulationTS+" | "+finalSimulationTS);
	}
	
	public String debugFoo(){
		return "DEBUG Foo: "+ initialSimulationTS+" | "+finalSimulationTS;
	}

	public void start() {
		if (!alreadyStarted) {
			database = connectToDB("localhost", "5432", "lumina_db", "postgres", "root");
			meterDatabaseTable = getMeterDatabaseTable(meter);
			tsIndexPair = getInitialMeasureTimestamp(meterDatabaseTable);
			rr = new RoadRunner();
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
		
		
		@Override
		public void run() {
			long delta = 0;
			simulationStartTime = System.currentTimeMillis();
			while (true) {
				shouldIkeepWalking(); // to avoid active waiting when sim. is paused 
				EnergyMeasureTupleDTO tupleDTO = getDatastreamTupleByTimestamp(tsIndexPair.getFirstTS());
				pushDatastreamToClients(tupleDTO);
				if(simulationFirstTS == null){ //first record
					simulationFirstTS = tupleDTO.getMeasureTS();
				}
				delta = getDeltaBetweenTuples(tsIndexPair.getFirstTS(), tsIndexPair.getSecondTS());
				if (delta == -1) { // if pairs 2nd element == null, meaning that 1st is the last database record
					simulationLastTS = tsIndexPair.getFirstTS();
					String duration = milisecondsTo_HH_MM_SS_format(System.currentTimeMillis() - simulationStartTime);
					System.out.println("Simulation completed! From "+simulationFirstTS+" to "+simulationLastTS+" in "+duration+"ms");
					return;
				}
				tsIndexPair = getNextTwoMeasureTimestamps(meterDatabaseTable, tsIndexPair.getFirstTS());
				try {
					Thread.sleep(delta/speedTimeFactor);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		public synchronized void resumeRunner(){
			keepWalking = true;
			notifyAll();
		}
		
		public void pauseRunner(){
			keepWalking = false;
		}
		
		private synchronized void shouldIkeepWalking(){
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

	private EnergyMeasureTupleDTO getDatastreamTupleByTimestamp(String targetTS) {
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
		long res;
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

	private TimestampIndexPair getNextTwoMeasureTimestamps(String meterDatabaseTable, String pivotTimestamp) {
		String queryStatement = "SELECT measure_timestamp " + 
								"FROM " + meterDatabaseTable + 
								"WHERE measure_timestamp > " + "\'" + pivotTimestamp + "\'" +
								" ORDER BY measure_timestamp ASC " + "LIMIT 2";
		return executeQueryAndBuildResultPair(queryStatement);
	}

	private TimestampIndexPair getInitialMeasureTimestamp(String meterDatabaseTable) {
		String queryStatement = "SELECT measure_timestamp " + 
								"FROM " + meterDatabaseTable + 
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

	private String getMeterDatabaseTable(EnergyMeter em) {
		switch (em) {
			case LIBRARY:
				return " simulator.library_measures ";
			case LECTUREHALL_A4:
				return " simulator.lecturehall_a4_measures ";
			case LECTUREHALL_A5:
				return " simulator.lecturehall_a5_measures ";
			case CLASSROOM_1_17:
				return " simulator.\"classroom_1.17_measures\" ";
			case CLASSROOM_1_19:
				return " simulator.\"classroom_1.19_measures\" ";
			case DEPARTMENT_14:
				return " simulator.department_14_measures ";
			case DEPARTMENT_16:
				return " simulator.department_16_measures ";
			case LAB_1_58_MIT:
				return " simulator.\"lab_1.58_mit_measures\" ";
			case UTA_A4:
				return " simulator.uta_a4_measures ";
			case TEST:
				return " simulator_test.test ";
			default:
				return null;
		}
	}

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
	
	private TimestampIndexPair validateInputTimestamps(String initialTS, String finalTS) throws Exception{
		TimestampIndexPair resPair = new TimestampIndexPair();
		if(getDeltaBetweenTuples(finalTS, initialTS) > 0){
			throw new Exception("[Error]: initialTS > finalTS. This does not make sense!");
		}
		//function will be placed in *res* TimestampIndexPair
		getNearestMeasureTimestamp(initialTS, resPair);
		if(resPair.getFirstTS()==null){
			throw new Exception("[Error]: There is any ts >= "+initialTS+" valid in the Database.");
		}
		getNearestMeasureTimestamp(finalTS, resPair);
		if(resPair.getSecondTS()==null){
			throw new Exception("[Error]: There is any ts >= "+finalTS+" valid in the Database.");
		}
		return resPair;
	} 

	
}// SimulatorImpl class EOF
