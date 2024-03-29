package server;
/**
 * The Server class implements an application that acts as a 
 * server that will receive and respond to TFTP requests.
 * 
 * @author  Loktin Wong
 * @version 1.0.1
 * @since 25-01-2016
 */

import java.util.Scanner;

import Configuration.Configuration;

public class Server {
  /**
   * Main method which creates an instance of Server to receive TFTP requests.
   * 
   * @param args
   * @throws InterruptedException
   */
  public static void main(String[] args) {
    Listener listener = new Listener(Configuration.SERVER_PORT);
    Thread listenThread = new Thread(listener, "Listener");
    listenThread.start();

    Scanner sc = new Scanner(System.in);
    String command = null;
    boolean shutdown = false;

    while (!shutdown) {
      command = sc.nextLine();
      if (command.equals("shutdown")) {
        shutdown = true;
        listener.requestStop();
      }
    }
    sc.close();
    System.out.println("[SYSTEM] Waiting for active connections to terminate (if any)");
  }
}
