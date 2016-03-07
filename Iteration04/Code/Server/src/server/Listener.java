package server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import Configuration.Configuration;

/**
 * The Listener class is implemented in order to allow the Server application
 * to continue to accept input while still listening for Client requests
 * 
 * @author Loktin Wong
 * @version 1.0.1
 * @since 25-01-2016
 */
class Listener implements Runnable {
  private DatagramSocket receiveSocket;
  private boolean stopRequested;
  
  /**
   * Default Listener constructor instantiates a DatagramSocket using the
   * SERVER_PORT constant defined in the Server class.
   * 
   * @param port
   */
  public Listener(int port) {
    try {
      this.receiveSocket = new DatagramSocket(port);
      this.stopRequested = false;
    } catch (SocketException e) {
      e.printStackTrace();
      System.exit(1); // TODO: add exception
    }
  }
  
  /**
   * This method listens for DatagramPackets and blocks until
   * one is received. Upon receiving a DatagramPacket, it calls
   * the processRequest(DatagramPacket) method to process the 
   * request.
   */ 
  public void run() {
    int connections = 0;
    
    while (!stopRequested()) {
      byte[] buffer = new byte[517];
      DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
      
      try {
        System.out.println("[SYSTEM] Listening for requests on port " + Configuration.SERVER_PORT);
        receiveSocket.receive(receivePacket);
      } catch (IOException e) { // catches an exception thrown by the Socket when it is closed via "shutdown" command
        System.out.println("[SYSTEM] Listener shut down. No longer accepting new connections.");
        return;
      }
      
      int dataLength = receivePacket.getLength();
      byte[] receivedData = new byte[dataLength];
      System.arraycopy(receivePacket.getData(), 0, receivedData, 0, dataLength);
      
      System.out.println("[SYSTEM] New Connection request received, creating new Thread.");
      
      Thread userConnection = new Thread(new RequestHandler(receivePacket), "Connection #" + ++connections);
      userConnection.start();
    }
  }
  
  /**
   * Method allows the Server class to stop the Listener.
   * @throws InterruptedException 
   */
  public synchronized void requestStop() {
    stopRequested = true;
    receiveSocket.close();
  }
  
  /**
   * Getter for determining whether a stop has been requested
   * by the Server.
   * 
   * @return boolean 
   */
  private synchronized boolean stopRequested() {
    return stopRequested;
  }
}