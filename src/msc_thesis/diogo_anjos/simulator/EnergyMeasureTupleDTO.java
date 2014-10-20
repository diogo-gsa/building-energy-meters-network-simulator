package msc_thesis.diogo_anjos.simulator;
/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * 
 */
public class EnergyMeasureTupleDTO {

	private String measure_ts = null;
	private String meter_location = null;
	
	private Double ph1_ampere = null;
	private Double ph1_powerFactor = null;
	private Double ph1_volt = null;
	
	private Double ph2_ampere = null;
	private Double ph2_powerFactor = null;
	private Double ph2_volt = null;
	
	private Double ph3_ampere = null;
	private Double ph3_powerFactor = null;
	private Double ph3_volt = null;
	
	
	public EnergyMeasureTupleDTO(String ts, String location){
		measure_ts = ts;
		meter_location = location; 	
	} 
	
	public void setPh1Ampere(String value){
		ph1_ampere = stringToDouble(value);
	}
	
	public void setPh1PowerFactor(String value){
		ph1_powerFactor = stringToDouble(value);
	}
	
	public void setPh1Volt(String value){
		ph1_volt = stringToDouble(value);
	}
		
	public void setPh2Ampere(String value){
		ph2_ampere = stringToDouble(value);
	}
	
	public void setPh2PowerFactor(String value){
		ph2_powerFactor = stringToDouble(value);
	}
	
	public void setPh2Volt(String value){
		ph2_volt = stringToDouble(value);
	}
	
	public void setPh3Ampere(String value){
		ph3_ampere = stringToDouble(value);
	}
	
	public void setPh3PowerFactor(String value){
		ph3_powerFactor = stringToDouble(value);
	}
	
	public void setPh3Volt(String value){
		ph3_volt = stringToDouble(value);
	}
	
	public String getMeasureTS() {
		return measure_ts;
	}

	public String getMeterLocation() {
		return meter_location;
	}

	public Double getPh1Ampere() {
		return ph1_ampere;
	}

	public Double getPh1PowerFactor() {
		return ph1_powerFactor;
	}

	public Double getPh1Volt() {
		return ph1_volt;
	}

	public Double getPh2Ampere() {
		return ph2_ampere;
	}

	public Double getPh2PowerFactor() {
		return ph2_powerFactor;
	}

	public Double getPh2Volt() {
		return ph2_volt;
	}

	public Double getPh3Ampere() {
		return ph3_ampere;
	}

	public Double getPh3PowerFactor() {
		return ph3_powerFactor;
	}

	public Double getPh3Volt() {
		return ph3_volt;
	}

	public Double getPh1Consumption(){
		return ph1_ampere*ph1_powerFactor*ph1_volt;
	}
	
	public Double getPh2Consumption(){
		return ph2_ampere*ph2_powerFactor*ph2_volt;
	}
	
	public Double getPh3Consumption(){
		return ph3_ampere*ph3_powerFactor*ph3_volt;
	}

	private double stringToDouble(String value){
		return Double.parseDouble(value);
	}
	
	@Override
	public String toString() {
		return "<ts="+measure_ts+",location="+meter_location+
				",ph1{a="+ph1_ampere+",pf="+ph1_powerFactor+",v="+ph1_volt+"}="+getPh1Consumption()+"," +
				"ph2{a="+ph2_ampere+",pf="+ph2_powerFactor+",v="+ph2_volt+"}="+getPh2Consumption()+"," +
				"ph3{a="+ph3_ampere+",pf="+ph3_powerFactor+",v="+ph3_volt+"}="+getPh3Consumption()+">";
	}
	
}
