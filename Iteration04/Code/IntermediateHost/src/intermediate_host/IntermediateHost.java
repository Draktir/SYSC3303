package intermediate_host;
/**
 * The IntermediateHost class implements an application 
 * that acts as both a client, and a server that will 
 * receive and forward TFTP requests to their intended
 * destinations.
 * 
 * @author  Loktin Wong
 * @author  Philip Klostermann
 * @version 1.0.0
 * @since 22-01-2016
 */

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import configuration.Configuration;
import modification.*;

public class IntermediateHost {
  private DatagramSocket clientSocket;
  private PacketModifier packetModifier;

  /**
   * Main method which creates an instance of IntermediateHost to forward and
   * receive TFTP requests.
   * 
   * @param args
   */
  public static void main(String[] args) {
    IntermediateHost h = new IntermediateHost();
    Scanner scan = new Scanner(System.in);
    do {
      h.go();
      System.out.println("\nSimulation complete!\n");
      System.out.print("Do you want to simulate another error scenario? (y/n) ");
    } while (scan.next().equalsIgnoreCase("y"));
    scan.close();
  }

  public void go() {
    List<Thread> connectionThreads = new ArrayList<>();
    boolean hasRun = false;
    
    // Show the Modification configuration menu
    ModificationMenu modMenu = new ModificationMenu();
    // .show() returns a packetModifier based on the user's configuration
    packetModifier = modMenu.show();

    try {
      clientSocket = new DatagramSocket(Configuration.INTERMEDIATE_PORT);
      clientSocket.setSoTimeout(1000);
    } catch (SocketException e1) {
      e1.printStackTrace();
      return;
    }

    log("Waiting for client requests on port " + Configuration.INTERMEDIATE_PORT);

    do {
      byte[] buffer = new byte[1024];
      DatagramPacket requestDatagram = new DatagramPacket(buffer, buffer.length);
  
      try {
        clientSocket.receive(requestDatagram);
      } catch (SocketTimeoutException e) {
        continue;
      } catch (IOException e) {
        e.printStackTrace();
      }  
      
      log("Received packet");
      printPacketInformation(requestDatagram);
  
      Runnable tftpTransfer = new TftpTransfer(requestDatagram, packetModifier);
      Thread t = new Thread(tftpTransfer, "#" + (connectionThreads.size() + 1));
      connectionThreads.add(t);
      t.start();
      hasRun = true;
    } while (!hasRun || connectionThreads.stream().anyMatch((t) -> t.isAlive()));
    
    clientSocket.close();
    log("All connections terminated");
  }

  /**
   * Prints out request contents as a String and in bytes.
   * 
   * @param buffer
   */
  public static void printPacketInformation(DatagramPacket packet) {
    byte[] data = new byte[packet.getLength()];
    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
    String contents = new String(data);
    
    System.out.println("\n-------------------------------------------");
    System.out.println("\tAddress: " + packet.getAddress());
    System.out.println("\tPort: " + packet.getPort());
    System.out.println("\tPacket contents: ");
    System.out.println("\t" + contents.replaceAll("\n", "\t\n"));

    System.out.println("\tPacket contents (bytes): ");
    System.out.print("\t");
    for (int i = 0; i < data.length; i++) {
      System.out.print(data[i] + " ");
    }
    System.out.println("\n-------------------------------------------\n");
  }
  
  private void log(String msg) {
    System.out.println("[INTERMEDIATE] " + msg);
  }
}
