package msc_thesis.diogo_anjos.client;

/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */

import java.text.ParseException;

import msc_thesis.diogo_anjos.simulator.EnergyMeasureTupleDTO;
import msc_thesis.diogo_anjos.simulator.EnergyMeter;
import msc_thesis.diogo_anjos.simulator.impl.SimulatorImpl;

public class SimulatorClient {

	public static void main(String[] args) throws ParseException, InterruptedException {

		SimulatorImpl sim = new SimulatorImpl(EnergyMeter.TEST);
		sim.start();
		
//		EnergyMeasureTupleDTO tuple = new EnergyMeasureTupleDTO("2014-01-01 00:00:01", "Biblioteca");  
//		tuple.setPh1Ampere("3.3");
//		tuple.setPh1PowerFactor("0");
//		tuple.setPh1Volt("5");
//		
//		double resMul = tuple.getPh1Ampere()*tuple.getPh1PowerFactor()*tuple.getPh1Volt();
//		double resSum = tuple.getPh1Ampere()+tuple.getPh1PowerFactor()+tuple.getPh1Volt();
//		
//		System.out.println("mul="+resMul+" | "+"sum="+resSum);
		
	}


}
