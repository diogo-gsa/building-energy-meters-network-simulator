package msc_thesis.diogo_anjos.simulator;

public interface SimulatorClient {
		
	public void receiveDatastream(EnergyMeasureTupleDTO tuple);
	public void simulationHasFinishedNotification();
	
}
