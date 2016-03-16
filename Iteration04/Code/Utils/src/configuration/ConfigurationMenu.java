package configuration;

import java.util.InputMismatchException;
import java.util.Scanner;

public class ConfigurationMenu {
	// configurable parameters with default values
	private int timeoutTime = 5000;
	private int maxRetries = 3;
	private int intermediatePort = 68;
	private int clientConnectToPort = intermediatePort;
	private int serverPort = 69;
	private int blockSize = 512;
	private String clientPath = "";
	private String serverPath = "";

	private Scanner sc = new Scanner(System.in);
	
	public void show() {
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
						Configuration.set(true, timeoutTime, maxRetries, clientConnectToPort, 
								intermediatePort, serverPort, blockSize, clientPath, serverPath);
						modeSet = true;
						break;
					case 2: // Ignores intermediate host, very verbose.
						Configuration.set(true, timeoutTime, maxRetries, serverPort, 
								intermediatePort, serverPort, blockSize, clientPath, serverPath);
						modeSet = true;
						break;
					case 3: // Ignores intermediate host, very little output.
						Configuration.set(false, timeoutTime, maxRetries, serverPort, 
								intermediatePort, serverPort, blockSize, clientPath, serverPath);
						modeSet = true;
						break;
					case 4:	// Debug mode for Linux as it requires root permissions to use ports 68, 69
						Configuration.set(true, timeoutTime, maxRetries, 6800, 
								6800, 6900, blockSize, clientPath, serverPath);
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
		System.out.print("\nMODE: Do you want verbose output (y/n)? ");
		boolean verbose = sc.next().equals("y");
		
		System.out.print("TIMEOUT: Timeout time in ms (" + timeoutTime + "): ");
		timeoutTime = getPositiveIntOrDefault(timeoutTime);
		
		System.out.print("MAX_ATTEMPTS: Max attempts to re-send a timed out packet (" + maxRetries + "): ");
		maxRetries = getPositiveIntOrDefault(maxRetries);
		
		System.out.print("INTERMEDIATE_PORT: Port the intermediate host listens on (" + intermediatePort + "): ");
		intermediatePort = getPositiveIntOrDefault(intermediatePort);
		
		System.out.print("CLIENT_CONNECT_TO_PORT: Port the client sends a RRQ or WRQ to (" + clientConnectToPort + "): ");
		clientConnectToPort = getPositiveIntOrDefault(clientConnectToPort);
		
		System.out.print("SERVER_PORT: Port the server listens on (" + serverPort + "): ");
		serverPort = getPositiveIntOrDefault(serverPort);
		
		System.out.print("BLOCK_SIZE: Size of file blocks in bytes (" + blockSize + "): ");
		blockSize = getPositiveIntOrDefault(blockSize);
		
		System.out.print("CLIENT_PATH: Path the client stores files under, using '/' (" + clientPath + "): ");
		clientPath = getPathOrNothing();
		
		System.out.print("CLIENT_PATH: Path the server stores files under, using '/' (" + serverPath + "): ");
		serverPath = getPathOrNothing();
		
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
