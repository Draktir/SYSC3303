package configuration;

import java.util.InputMismatchException;
import java.util.Scanner;

public class ConfigurationMenu {
	private Scanner sc = new Scanner(System.in);
	
	public void show() {
		// get current / default configuration
		Configuration conf = Configuration.get();
		boolean modeSet = false;
		
		do {
			System.out.println("Select a mode to run in: ");
			System.out.println("  [ 1 ] Debug Mode  [uses Intermediate Host]");
			System.out.println("  [ 2 ] Test Mode   [ignores Intermediate Host; verbose]");
			System.out.println("  [ 3 ] Quiet Mode  [ignores Intermediate Host]");
			System.out.println("  [ 4 ] Linux Mode  [debug mode compatible with Linux]");
			System.out.println("  [ 5 ] Manual Mode [configure all parameters manually]");
			System.out.println("  [ 0 ] Exit");
			System.out.print(" > ");
			
			try {
				int m = sc.nextInt();
				
				switch (m) {
					case 0:
						System.exit(1);
						break;
					case 1: // Uses the intermediate host, verbose
						Configuration.set(true, conf.TIMEOUT_TIME, conf.MAX_RETRIES, 68, 
								68, 69, conf.BLOCK_SIZE, conf.CLIENT_PATH, conf.SERVER_PATH);
						modeSet = true;
						break;
					case 2: // Ignores intermediate host, very verbose.
						Configuration.set(true, conf.TIMEOUT_TIME, conf.MAX_RETRIES, 69, 
								conf.INTERMEDIATE_PORT, 69, conf.BLOCK_SIZE, conf.CLIENT_PATH, conf.SERVER_PATH);
						modeSet = true;
						break;
					case 3: // Ignores intermediate host, very little output.
						Configuration.set(false, conf.TIMEOUT_TIME, conf.MAX_RETRIES, 69, 
								conf.INTERMEDIATE_PORT, 69, conf.BLOCK_SIZE, conf.CLIENT_PATH, conf.SERVER_PATH);
						modeSet = true;
						break;
					case 4:	// Debug mode for Linux as it requires root permissions to use ports 68, 69
						Configuration.set(true, conf.TIMEOUT_TIME, conf.MAX_RETRIES, 6800, 
								6800, 6900, conf.BLOCK_SIZE, conf.CLIENT_PATH, conf.SERVER_PATH);
						modeSet = true;
					case 5:
						modeSet = configureManualMode();
						break;
					default:
						System.err.println("Invalid selection");
						modeSet = false;
						break;
				}
			} catch (InputMismatchException e) {
				e.printStackTrace();
				modeSet = false;
				System.err.println("An error has occured in the configuration setup.");
			}
		} while (modeSet == false);
	}
	
	private boolean configureManualMode() {
		// get current / default configuration
		Configuration conf = Configuration.get();
		
		System.out.print("\nMODE: Do you want verbose output (y/n)? ");
		boolean verbose = sc.next().equals("y");
		
		System.out.print("TIMEOUT: Timeout time in ms (" + conf.TIMEOUT_TIME + "): ");
		int timeoutTime = getPositiveIntOrDefault(conf.TIMEOUT_TIME);
		
		System.out.print("MAX_ATTEMPTS: Max attempts to re-send a timed out packet (" + conf.MAX_RETRIES + "): ");
		int maxRetries = getPositiveIntOrDefault(conf.MAX_RETRIES);
		
		System.out.print("INTERMEDIATE_PORT: Port the intermediate host listens on (" + conf.INTERMEDIATE_PORT + "): ");
		int intermediatePort = getPositiveIntOrDefault(conf.INTERMEDIATE_PORT);
		
		System.out.print("CLIENT_CONNECT_TO_PORT: Port the client sends a RRQ or WRQ to (" + conf.CLIENT_CONNECT_TO_PORT + "): ");
		int clientConnectToPort = getPositiveIntOrDefault(conf.CLIENT_CONNECT_TO_PORT);
		
		System.out.print("SERVER_PORT: Port the server listens on (" + conf.SERVER_PORT + "): ");
		int serverPort = getPositiveIntOrDefault(conf.SERVER_PORT);
		
		System.out.print("BLOCK_SIZE: Size of file blocks in bytes (" + conf.BLOCK_SIZE + "): ");
		int blockSize = getPositiveIntOrDefault(conf.BLOCK_SIZE);
		
		System.out.print("CLIENT_PATH: Path the client stores files under, using '/' (" + conf.CLIENT_PATH + "): ");
		String clientPath = getPathOrNothing();
		
		System.out.print("CLIENT_PATH: Path the server stores files under, using '/' (" + conf.SERVER_PATH + "): ");
		String serverPath = getPathOrNothing();
		
		Configuration.set(verbose, timeoutTime, maxRetries, clientConnectToPort, intermediatePort, serverPort, 
				blockSize, clientPath, serverPath);
		
		return true;
	}
	
	private int getPositiveIntOrDefault(int defaultValue) {
		int intVal = -1;
		
		do {
			String in = sc.nextLine();
			// if user just hits enter without entering anything
			if (in.length() == 0) {
				return defaultValue;
			}
			try {
				intVal = Integer.valueOf(in);
			} catch (NumberFormatException e) {
				intVal = -1;
			}
		} while (intVal < 0);
		
		return intVal;
	}
	
	private String getPathOrNothing() {
		String in = sc.nextLine();
		
		if (in.length() == 0) {
			return "";
		}
		
		if (in.charAt(in.length() - 1) != '/') {
			in += "/";
		}
		return in;
	}
	
}
