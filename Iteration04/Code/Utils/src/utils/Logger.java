package utils;

import java.util.Date;

import configuration.Configuration;
import configuration.Configuration.Mode;

public class Logger {
		String unitName;
	
		public Logger(String unitName){
			this.unitName = unitName;
		}

		public void log(String message){
			if (Configuration.applicationMode == Mode.DEBUG_MODE 
			    || Configuration.applicationMode == Mode.TEST_MODE
			    || Configuration.applicationMode == Mode.LINUX_MODE) {
				System.out.println(new Date().getTime() + " [" + this.unitName + " - " +
						 Thread.currentThread().getName() + "] " + message);
			}
		}
		
		public void logAlways(String message){
		  System.out.println(new Date().getTime() + " [" + this.unitName + " - " +
          Thread.currentThread().getName() + "] " + message);
			
		}
		public void logError(String message){
		  System.err.println(new Date().getTime() + " [" + this.unitName + " - " +
          Thread.currentThread().getName() + "] " + message);
		}
		 

		
}
