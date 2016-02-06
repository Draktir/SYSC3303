package server;
/**
 * The Server class implements an application that acts as a 
 * server that will receive and respond to TFTP requests.
 * 
 * @author  Loktin Wong
 * @version 1.0.1
 * @since 25-01-2016
 */

import java.net.SocketException;
import java.util.Scanner;

public class Server {
	public static final int SERVER_PORT = 69;
	public static enum Mode {TEST, NORMAL}; // TODO: implement
	Mode currentMode;
	
	/**
	 * Default Server class constructor sets the mode of the environment
	 * to TEST. The modes still need to be implemented if it is required
	 * for Iteration 1, if not then this needs to be removed.
	 */
	public Server() {
		this.currentMode = Mode.TEST; // TODO: implement
	}
	
	/**
	 * Main method which creates an instance of Server to receive 
	 * TFTP requests.
	 * 
	 * @param args unused
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws InterruptedException {
		Listener listener = new Listener(SERVER_PORT);
		Thread listenThread = new Thread(listener, "Listener");
		listenThread.start();
		
		Scanner sc = new Scanner(System.in);
		String command = null;
		
		while (true) {
			command = sc.nextLine();
			if (command.equals("shutdown")) {
			  try {
          listener.requestStop();
        } catch (SocketException e) {
          System.out.println("Listener shutdown.");
        }
			  sc.close();
			}
		}
	}
}






