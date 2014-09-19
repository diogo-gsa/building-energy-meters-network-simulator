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
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import msc_thesis.diogo_anjos.simulator.EnergyMeasureTupleDTO;
import msc_thesis.diogo_anjos.simulator.EnergyMeter;
import msc_thesis.diogo_anjos.simulator.Simulator;

public class SimulatorImpl implements Simulator {

	private Connection database = null;
	private String meterDatabaseTable = null;
	private TimestampIndexPair tsIndexPair = null;
	private boolean running = false;

	public SimulatorImpl(EnergyMeter em) {
		database = connectToDB("localhost", "5432", "lumina_db", "postgres", "root");
		meterDatabaseTable = getMeterDatabaseTable(em);
		tsIndexPair = getInitialMeasureTimestamp(meterDatabaseTable);

		//System.out.println("DEBUG: "+meterDatabaseTable+"|"+tsIndexPair+"\n");

	}

	public void start() throws InterruptedException {
		long delta = 0;
		long debug = 0; //DEBUG
		while (true) {
			if(debug != 0){ //DEBUG
				System.out.println("Elpased: "+(System.currentTimeMillis()-debug)+" ms");
			}	
			System.out.println("Input: " + tsIndexPair.getFirstTS()); //DEBUG
			EnergyMeasureTupleDTO tupleDTO = getDatastreamTupleByTimestamp(tsIndexPair.getFirstTS());
			System.out.println("Debug: "+tupleDTO+"\n"); //DEBUG /TODO estás aqui
			
			debug = System.currentTimeMillis(); //DEUG
			
			delta = getDeltaBetweenTuples(tsIndexPair.getFirstTS(), tsIndexPair.getSecondTS());
			if(delta == -1 ){
				System.out.println("The end of database has been reached. Simulation completed!");
				return;
			}
			tsIndexPair =  getNextTwoMeasureTimestamps(meterDatabaseTable, tsIndexPair.getFirstTS());
			Thread.sleep(delta);
		}

	}

	private EnergyMeasureTupleDTO getDatastreamTupleByTimestamp(String targetTS){
		String queryStatement = "SELECT * " + 
								"FROM " + meterDatabaseTable + 
								"WHERE measure_timestamp = "+"\'"+targetTS+"\'"+ 
								"LIMIT 1"; //just in case
		try {
			return buildDtoFromResultSet(executeQuery(queryStatement));
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	private EnergyMeasureTupleDTO buildDtoFromResultSet(ResultSet rs){
		EnergyMeasureTupleDTO resDTO = null;
		try {
			if(rs.next()) {
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

	private TimestampIndexPair getNextTwoMeasureTimestamps(String meterDatabaseTable, String pivotTimestamp){
		String queryStatement = "SELECT measure_timestamp " + 
								"FROM " + meterDatabaseTable + 
								"WHERE measure_timestamp > "+"\'"+pivotTimestamp+"\'"+
//								"WHERE measure_timestamp > " + "\'2014-01-01 00:00:01\'" +
								" ORDER BY measure_timestamp ASC " + 
								"LIMIT 2";
		return executeQueryAndBuildResultPair(queryStatement);
	}
	
	
	private TimestampIndexPair getInitialMeasureTimestamp(String meterDatabaseTable) {
		String queryStatement = "SELECT measure_timestamp " + 
								"FROM " + meterDatabaseTable + 
								"ORDER BY measure_timestamp ASC " + 
								"LIMIT 2";
		return executeQueryAndBuildResultPair(queryStatement);
	}

	private TimestampIndexPair executeQueryAndBuildResultPair(String queryStatement){
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

}
