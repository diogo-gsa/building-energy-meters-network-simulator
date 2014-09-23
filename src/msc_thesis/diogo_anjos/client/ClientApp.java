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
			 
//			 sim = new SimulatorImpl(EnergyMeter.TEST/*, "2014-01-01 00:00:17", "2014-01-01 00:00:04"*/); //TESTE 1 [Pass]
//			 Exception TSi > TSf (impossible) 
			 
			 sim = new SimulatorImpl(EnergyMeter.TEST/*, "2014-01-01 00:00:01", "2014-01-01 00:00:15"*/); //TESTE 2 [Pass]
			 System.out.println(sim.toString());
//			 OK! = 14 seconds
			 
//			 sim = new SimulatorImpl(EnergyMeter.TEST, "2014-01-01 00:00:05", "2014-01-01 00:00:15"); //TESTE 3 [Pass]
//			 System.out.println(sim.toString());	
//			 OK! = 9 seconds
			 
//			 sim = new SimulatorImpl(EnergyMeter.TEST, "2014-01-01 00:00:02", "2014-01-01 00:00:12"); //TESTE 4 [Pass]
//			 System.out.println(sim.toString());
//			 OK! = 13 seconds 
			 
//			 sim = new SimulatorImpl(EnergyMeter.TEST, "2014-01-01 00:00:05", "2014-01-01 00:00:10"); //TESTE 5 [Pass]
//			 System.out.println(sim.toString());
//			 OK! = 5 seconds
			 
//			 sim = new SimulatorImpl(EnergyMeter.TEST, "2014-01-01 00:00:07", "2014-01-01 00:00:16"); //TESTE 6
//			 System.out.println(sim.toString());
//			 Exception there is no upper bound (impossible)
			 
//			 sim = new SimulatorImpl(EnergyMeter.TEST, "2014-01-01 00:00:20", "2014-01-01 00:00:25"); //TESTE 7
//			 System.out.println(sim.toString());
//			 Exception there is no upper neither lower bound (impossible)
			 
			 
			 sim.registerNewClient(client);
			 sim.start();
			 
//			 Thread.sleep(5000);
//			 System.out.println(sim.toString());
			 
		}catch (Exception e) {
			e.printStackTrace();
		}	
	
		
		
	
	
	}
	
	
	@Override
	public void receiveDatastream(EnergyMeasureTupleDTO tuple) {
		System.out.println("ClientReceives: "+tuple);
	}

}
