package msc_thesis.diogo_anjos.client;

/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */

import msc_thesis.diogo_anjos.simulator.EnergyMeasureTupleDTO;
import msc_thesis.diogo_anjos.simulator.EnergyMeter;
import msc_thesis.diogo_anjos.simulator.Simulator;
import msc_thesis.diogo_anjos.simulator.SimulatorClient;
import msc_thesis.diogo_anjos.simulator.impl.SimulatorImpl;

public class ClientApp implements SimulatorClient {

	private long lastTS = 0;
	
	
	public static void main(String[] args)  {
		
		SimulatorClient client = new ClientApp();
				
		Simulator sim = null;
		try { 
			 sim = new SimulatorImpl(EnergyMeter.LIBRARY, "2014-03-17 08:00:00", "2014-03-17 08:05:00");
			 System.out.println(sim.toString());
			 sim.registerNewClient(client);
			 sim.setSpeedTimeFactor(1);
			 sim.start();
		}catch (Exception e) {
			e.printStackTrace();
		}	
	}
	
	
	@Override
	public void receiveDatastream(EnergyMeasureTupleDTO tuple) {
		//Debug
		long delta = 0;
		if(lastTS != 0){
			delta = (System.currentTimeMillis()-lastTS)/1000;
		}	
		System.out.println("ClientReceives: "+tuple+" delta: "+delta+" sec");
		lastTS = System.currentTimeMillis(); //Debug
	
	}

	@Override
	public void simulationHasStartedNotification(EnergyMeter em) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void simulationHasFinishedNotification(EnergyMeter em) {
		// TODO Auto-generated method stub	
	}
}
