package msc_thesis.diogo_anjos.simulator;
/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */


public interface Simulator {

	public void pushDatastreamToClients(EnergyMeasureTupleDTO tuple);
	public void registerNewClient(SimulatorClient client);
	
}
