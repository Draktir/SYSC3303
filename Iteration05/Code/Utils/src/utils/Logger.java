package utils;

import java.util.Calendar;

import configuration.Configuration;

/**
 * Logger class that prints logging info to console.
 * 
 */

public class Logger {
	String unitName;

	public Logger(String unitName) {
		this.unitName = unitName;
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

	private String getTime() {
		Calendar calendar = Calendar.getInstance();
		
		String h = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
		String m = String.format("%02d", calendar.get(Calendar.MINUTE));
		String s = String.format("%02d", calendar.get(Calendar.SECOND));
		String ms = String.format("%03d", calendar.get(Calendar.MILLISECOND));

		return h + ":" + m + ":" + s + ":" + ms;
	}
}
