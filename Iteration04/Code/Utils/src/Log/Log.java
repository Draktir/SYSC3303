package Log;

import com.sun.jmx.snmp.Timestamp;
import configuration.Configuration;
import configuration.Configuration.Mode;

public class Log {
		String teststring;
	
		Log(String s){
			teststring =s; 
		}
	
		public void log(String information){
			//if isTest is true that means we are in the test mode; and print everything//DEBUG_MOde or TEST_MODE
			if (Configuration.applicationMode == Mode.DEBUG_MODE || Configuration.applicationMode == Mode.TEST_MODE){
				System.out.println(new Timestamp().getDateTime() + "[ " + this.teststring + 
						 Thread.currentThread().getName() + " ]" + information);
			}
		}
		
		public void logalways(String information){
			System.out.println(new Timestamp().getDateTime() + "[ " + this.teststring + 
					 Thread.currentThread().getName() + " ]" + information);
			
		}
		public void logerr(String information){
			System.err.println(new Timestamp().getDateTime() + "[ " + this.teststring + 
				 Thread.currentThread().getName() + " ]" + information);}
		 

		
}
