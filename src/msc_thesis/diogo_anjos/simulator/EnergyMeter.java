package msc_thesis.diogo_anjos.simulator;
/*
 * @author Diogo Anjos (diogo.silva.anjos@tecnico.ulisboa.pt)
 * MScThesis Solution:  Real-Time Data Processing Architecture 
 * 						for Energy Management Applications
 */

public enum EnergyMeter {
	
	// The Real Time Series
	LIBRARY			(" simulator.library_measures "),
    LECTUREHALL_A4	(" simulator.lecturehall_a4_measures "),
    LECTUREHALL_A5	(" simulator.lecturehall_a5_measures "),
    CLASSROOM_1_17	(" simulator.\"classroom_1.17_measures\" "),
    CLASSROOM_1_19	(" simulator.\"classroom_1.19_measures\" "),
    DEPARTMENT_14	(" simulator.department_14_measures "),
    DEPARTMENT_16	(" simulator.department_16_measures "),
    LAB_1_58_MIT	(" simulator.\"lab_1.58_mit_measures\" "),
    UTA_A4			(" simulator.uta_a4_measures "),
    
    // TEST tables
    TEST_FIRST			(" simulator_test.test "),
    TEST_REPEATED		(" simulator_test.test_repeated_values "),
    TEST_EMPTY			(" simulator_test.test_empty "),
    TEST_ONLY_ONE_TUPLE	(" simulator_test.test_only_one_tuple "),
    TEST_TWO_TUPLES		(" simulator_test.test_two_tuples "),
    TEST_THREE_TUPLES	(" simulator_test.test_three_tuples ");

    
	// each energy meter database table
	private String addr; 
	
	EnergyMeter(String addr){
		this.addr = addr;
	}
	
	public String getDatabaseTable(){
		return addr;
	}
			
    

}
