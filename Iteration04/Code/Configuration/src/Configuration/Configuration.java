package Configuration;

public class Configuration {
  // Timeout time for all sockets in milliseconds.
  public static final int TIMEOUT_TIME = 500;
  // Max retries before we give up
  public static final int MAX_RETRIES = 3;
  // Port the intermediate host is listening on for requests
  public static final int INTERMEDIATE_PORT = 68; // grab from intermediatehost 
  // Port the server is listening on for requests
  public static final int SERVER_PORT = 69; // grab from intermediatehost
}
