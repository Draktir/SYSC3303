package configuration;

import java.util.InputMismatchException;
import java.util.Scanner;

public class Configuration {
	public static enum Mode {
		DEBUG_MODE, TEST_MODE, QUIET_MODE, LINUX_MODE
	};
	
	public static Mode applicationMode;

	public static int TIMEOUT_TIME = 5000;	// Timeout time for all sockets in milliseconds.
	public static int MAX_RETRIES = 3; 		// Max retries before we give up
	public static int INTERMEDIATE_PORT;	// Port the intermediate host is listening on for requests
	public static int SERVER_PORT;			// Port the server is listening on for requests
	public static int BLOCK_SIZE = 512;		// Block size of file data

	public static boolean setMode() {
		boolean modeSet = false;
		Scanner sc = new Scanner(System.in);
		
		do {
			System.out.println("Select a mode to run in: ");
			System.out.println("  [ 1 ] Debug Mode [uses Intermediate Host]");
			System.out.println("  [ 2 ] Test Mode  [ignores Intermediate Host; verbose]");
			System.out.println("  [ 3 ] Quiet Mode [ignores Intermediate Host]");
			System.out.println("  [ 4 ] Linux Mode [debug mode compatible with Linux]");
			System.out.println("  [ 0 ] Exit");
			System.out.print(" > ");
			
			try {
				int m = sc.nextInt(); // Could add more error checking here.
				
				switch (m) {
					case 1: // Uses the intermediate host
						applicationMode = Mode.DEBUG_MODE;
						SERVER_PORT = 69;
						INTERMEDIATE_PORT = 68;
						modeSet = true;
						break;
					case 2: // Ignores intermediate host, very verbose.
						// TODO currently the client reads the "server port" from INTERMEDIATE_PORT
						applicationMode = Mode.TEST_MODE;
						SERVER_PORT = 68;
						INTERMEDIATE_PORT = SERVER_PORT;
						modeSet = true;
						break;
					case 3: // Ignores intermediate host, very little output.
						// TODO currently the client reads the "server port" from INTERMEDIATE_PORT
						applicationMode = Mode.QUIET_MODE;
						SERVER_PORT = 68;
						INTERMEDIATE_PORT = SERVER_PORT;
						modeSet = true;
						break;
					case 4:	// Debug mode for Linux as it reserves ports 68, 69 for TFTP
						applicationMode = Mode.LINUX_MODE;
						INTERMEDIATE_PORT = 6800;
						SERVER_PORT = 6900;
						modeSet = true;
					default:
						break;
				}
			} catch (InputMismatchException e) {
				e.printStackTrace();
				modeSet = false;
				System.err.println("An error has occured in the configuration setup.");
			}
		} while (modeSet == false);
		
		
		return modeSet;
	}
	
	public static Mode getMode() {
		return applicationMode;
	}
}
