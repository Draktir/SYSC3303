package utils;

import java.util.Calendar;

import configuration.Configuration;

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
		if (Configuration.get().VERBOSE) {
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
		int h = calendar.get(Calendar.HOUR_OF_DAY);
		int m = calendar.get(Calendar.MINUTE);
		int s = calendar.get(Calendar.SECOND);
		int ms = calendar.get(Calendar.MILLISECOND);

		return h + ":" + m + ":" + s + ":" + ms;
	}
}
