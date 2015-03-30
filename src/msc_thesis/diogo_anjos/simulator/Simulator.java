package msc_thesis.diogo_anjos.simulator;

/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */

public interface Simulator {

	/*
	 *  Register a new simulator's client. Those clients will be notified 
	 *  (push mechanism) whenever a new measure event is produced by the simulator.
	 */
	public void registerNewClient(SimulatorClient client);
	
	
	/*
	 *   Start the simulation process, the production of energy event measures. 
	 *   This method has to be used at least one during the simulation life cycle process. 
	 */
	public void start();
	
	/*
	 * Stop the simulation process. Stop the production of new event measures. start() method 
	 * should be used to resume the simulation process, the database iterator index will 
	 * continue from the last produced event measure. stop() can not be used without the 
	 * previous usage of start()
	 */
	public void stop();
	
	/*
	 * Specify the time scale that should be used between each produced event measure, during the 
	 * time simulation. The Default value is 1, meaning that  the time interval between event 
	 * measures will not be shrunk. For instance, for an interval of 60 sec. (60k ms) between 
	 * tuple T1 and T2, the simulator will produce T1 and wait 6000 ms until produce T2 (1:1 time-scale). 
	 * setSpeedTimeFactor(int newFactor) readjusts the time-scale as follows: 1:newFactor, i.e. a 60 seconds 
	 * simulation period with a time factor of 2, will be complete within 30 seconds.
	 * The simulator internal unit time is the millisecond (ms).
	 * 
	 *	Time factor = 24 : Simulation process of a 24 hour's time series will be completed in 1hour.
	 *				= 720: Simulation process of a 1 Mounth's (720h) time series will be completed in 1hour.
	 */
	public boolean setSpeedTimeFactor(int newFactor);
	
	/*
	 * Get the current time scale factor (divisor).
	 */
	public int getSpeedTimeFactor();
	
	/*
	 * Release all gathered resources during the object construction.
	 * In this case, releases the setlled database connection. 
	 */
	public void destroy();
	
	/*
	 * An overrided toString() with the table and time range handled
	 * for this simulator instance.
	 */
	public String toString();
}
