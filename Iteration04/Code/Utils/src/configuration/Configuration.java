package configuration;

public class Configuration {
	private static Configuration instance = null;
	
	public final boolean VERBOSE; 	// print all logging output true/false.
	public final int TIMEOUT_TIME;	// Timeout time for all sockets in milliseconds.
	public final int MAX_RETRIES; 		// Max retries before we give up
	public final int CLIENT_CONNECT_TO_PORT;	// Port the client will send a request to
	public final int INTERMEDIATE_PORT; // port the intermediate host will listen on
	public final int SERVER_PORT;	// Port the server is listening on for requests
	public final int BLOCK_SIZE;		// Block size of file data
	public final String CLIENT_PATH; // path client will store files under, trailing slash required if not empty
	public final String SERVER_PATH; // path server will store files under, trailing slash required if not empty

	// default constructor assigns default values
	public Configuration() {
		VERBOSE = true;
		TIMEOUT_TIME = 5000;
		MAX_RETRIES = 3;
		CLIENT_CONNECT_TO_PORT = 68;
		INTERMEDIATE_PORT = 68;
		SERVER_PORT = 69;
		BLOCK_SIZE = 512;
		CLIENT_PATH = "";
		SERVER_PATH = "";
	}
	
	public Configuration(boolean verbose, int timeoutTime, int maxRetries, int clientConnectToPort, int intermediatePort,
			int serverPort, int blockSize, String clientPath, String serverPath) {
		VERBOSE = verbose;
		TIMEOUT_TIME = timeoutTime;
		MAX_RETRIES = maxRetries;
		CLIENT_CONNECT_TO_PORT = clientConnectToPort;
		INTERMEDIATE_PORT = intermediatePort;
		SERVER_PORT = serverPort;
		BLOCK_SIZE = blockSize;
		CLIENT_PATH = clientPath;
		SERVER_PATH = serverPath;
	}
	
	
	/**
	 * globally sets the given configuration
	 * 
	 * @param verbose
	 * @param timeoutTime
	 * @param maxRetries
	 * @param clientConnectToPort
	 * @param intermediatePort
	 * @param serverPort
	 * @param blockSize
	 * @param clientPath
	 * @param serverPath
	 */
	public static void set(boolean verbose, int timeoutTime, int maxRetries, int clientConnectToPort, int intermediatePort,
			int serverPort, int blockSize, String clientPath, String serverPath) {
		
		Configuration.instance = new Configuration(verbose, timeoutTime, maxRetries, clientConnectToPort, intermediatePort, 
				serverPort, blockSize, clientPath, serverPath);
	}
	
	/**
	 * returns the currently active configuration
	 * @return Configuration
	 */
	public static Configuration get() {
		if (instance == null) {
			// if nothing has been configured, create a default configuration
			instance = new Configuration();
		}
		return instance;
	}
	
}
