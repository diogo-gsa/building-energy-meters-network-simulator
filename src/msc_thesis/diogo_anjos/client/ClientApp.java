package msc_thesis.diogo_anjos.client;

/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */

import java.text.ParseException;

import msc_thesis.diogo_anjos.simulator.EnergyMeasureTupleDTO;
import msc_thesis.diogo_anjos.simulator.EnergyMeter;
import msc_thesis.diogo_anjos.simulator.SimulatorClient;
import msc_thesis.diogo_anjos.simulator.impl.SimulatorImpl;

public class ClientApp implements SimulatorClient {

	public static void main(String[] args) throws ParseException, InterruptedException {

		SimulatorClient client = new ClientApp();
		
		SimulatorImpl sim = new SimulatorImpl(EnergyMeter.TEST);
		sim.registerNewClient(client);
		sim.start();
		
	}

	@Override
	public void receiveDatastream(EnergyMeasureTupleDTO tuple) {
		System.out.println("ClientReceives: "+tuple+"\n");
	}


}
