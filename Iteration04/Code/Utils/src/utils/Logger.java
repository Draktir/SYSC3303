package utils;

import java.util.Calendar;

import configuration.Configuration;
import configuration.Configuration.Mode;

/**
 * Logger class that prints logging info to console.
 * 
 */

public class Logger {
	String unitName;
	Calendar calendar;

	public Logger(String unitName) {
		this.unitName = unitName;
		this.calendar = Calendar.getInstance();
	}

	public void log(String message) {
		if (Configuration.getMode() == Mode.DEBUG_MODE || Configuration.getMode() == Mode.TEST_MODE
				|| Configuration.getMode() == Mode.LINUX_MODE) {
			System.out.println(
					getTime() + " [" + this.unitName + " - " + Thread.currentThread().getName() + "] " + message);
		}
	}

	public void logAlways(String message) {
		System.out.println(
				getTime() + " [" + this.unitName + " - " + Thread.currentThread().getName() + "] " + message);

	}

	public void logError(String message) {
		System.err.println(
				getTime() + " [" + this.unitName + " - " + Thread.currentThread().getName() + "] " + message);
	}

	public String getTime() {
		String str = null;
		int h = calendar.get(Calendar.HOUR_OF_DAY) % 12;
		int m = calendar.get(Calendar.MINUTE);
		int s = calendar.get(Calendar.SECOND);
		int timeOfDay = calendar.get(Calendar.AM);

		str = h + ":";
		str += m + ":";
		str += s;

		if (timeOfDay == 0)
			str += "AM";
		if (timeOfDay == 1)
			str += "PM";

		return str;
	}
}
