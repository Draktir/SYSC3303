package Log;

import com.sun.jmx.snmp.Timestamp;
public class Log {
		String teststring;
		boolean isTest;
		Log(String s, boolean isT){
			isTest = isT;
			teststring =s; 
		}
	
		public void log(String information){
			//if isTest is true that means we are in the test mode; and print everything//DEBUG_MOde or TEST_MODE
			if(isTest){
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
