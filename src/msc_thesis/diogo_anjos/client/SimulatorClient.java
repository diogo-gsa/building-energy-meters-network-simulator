package msc_thesis.diogo_anjos.client;

/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */

import java.text.ParseException;

import msc_thesis.diogo_anjos.simulator.EnergyMeter;
import msc_thesis.diogo_anjos.simulator.impl.SimulatorImpl;

public class SimulatorClient {

	public static void main(String[] args) throws ParseException, InterruptedException {

		SimulatorImpl sim = new SimulatorImpl(EnergyMeter.TEST);
		sim.start();
	}


}
