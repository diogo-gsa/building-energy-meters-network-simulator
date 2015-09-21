package msc_thesis.diogo_anjos.simulator;

/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * MScThesis Solution:  Real-Time Data Processing Architecture 
 * 						for Energy Management Applications
 */

public interface SimulatorClient {
		
	public void receiveDatastream(EnergyMeasureTupleDTO tuple);
	public void simulationHasStartedNotification(EnergyMeter em);
	public void simulationHasFinishedNotification(EnergyMeter em);

}
