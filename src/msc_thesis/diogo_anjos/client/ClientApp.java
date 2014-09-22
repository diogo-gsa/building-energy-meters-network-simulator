package msc_thesis.diogo_anjos.client;

/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */

import msc_thesis.diogo_anjos.simulator.EnergyMeasureTupleDTO;
import msc_thesis.diogo_anjos.simulator.EnergyMeter;
import msc_thesis.diogo_anjos.simulator.SimulatorClient;
import msc_thesis.diogo_anjos.simulator.impl.SimulatorImpl;

public class ClientApp implements SimulatorClient {

	public static void main(String[] args)  {
		
		SimulatorClient client = new ClientApp();
				
		SimulatorImpl sim = null;
		try { 
			 sim = new SimulatorImpl(EnergyMeter.TEST, "2014-01-01 00:00:00", "2014-01-01 00:00:15");
			 sim.registerNewClient(client);
			 sim.start();
		}catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	
	@Override
	public void receiveDatastream(EnergyMeasureTupleDTO tuple) {
		System.out.println("ClientReceives: "+tuple);
	}

}
