package msc_thesis.diogo_anjos.simulator;
/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */


public interface Simulator {

	public void registerNewClient(SimulatorClient client);
	
	public void start();
	
	public void stop();
	
	public boolean setSpeedTimeFactor(int newFactor);
	
	public int getSpeedTimeFactor();
	
	public void destroy();

}
