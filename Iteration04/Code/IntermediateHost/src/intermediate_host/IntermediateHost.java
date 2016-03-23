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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import configuration.Configuration;
import configuration.ConfigurationMenu;
import modification.*;
import packet.InvalidRequestException;
import packet.Packet;
import packet.PacketParser;
import packet.Request;
import packet.Request.RequestType;
import utils.PacketPrinter;

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
    
    // let user choose a configuration
    new ConfigurationMenu().show();
    
    // Show the Modification configuration menu
    ModificationMenu modMenu = new ModificationMenu();
    // .show() returns a packetModifier based on the user's configuration
    packetModifier = modMenu.show();

    try {
      clientSocket = new DatagramSocket(Configuration.get().INTERMEDIATE_PORT);
      clientSocket.setSoTimeout(1000);
    } catch (SocketException e1) {
      e1.printStackTrace();
      return;
    }

    log("Waiting for client requests on port " + Configuration.get().INTERMEDIATE_PORT);

    AtomicBoolean keepAlive = new AtomicBoolean(false);
    
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
      PacketPrinter.print(requestDatagram);
  
      /*
       * START of DISGUSTINGNESS
       */
      PacketParser parser = new PacketParser();
      Request req = null;
      try {
        req = parser.parseRequest(requestDatagram);
      } catch (InvalidRequestException e) {}
      
      
      /* 
       * 
       * This if block is DISGUSTING. If we have time we REALLY should make this better!
       * 
       */
      if (req != null) {
      	
      	/* 
      	 * consumer that handles a delayed packet by starting a new TftpTransfer thread with
      	 * a duplicated DatagramPacket
      	 */
        Consumer<Packet> delayedPacketConsumer = (p) -> {
      		byte[] data = p.getPacketData();
          DatagramPacket dp = new DatagramPacket(data, data.length, 
              p.getRemoteHost(), p.getRemotePort());
          Runnable tftpTransfer = new TftpTransfer(dp, packetModifier);
          Thread t = new Thread(tftpTransfer, "#" + (connectionThreads.size() + 1));
          connectionThreads.add(t);
          t.start();
          keepAlive.set(false);
      	};
      	
      	if (req.type() == RequestType.READ) {
      		ReadRequestModification rrqMod = packetModifier.getRrqModification();
      		
      		if (rrqMod != null) {
          	int recvPort = Configuration.get().INTERMEDIATE_PORT;
          	
            if (rrqMod.getDelayModification() != null) {
            	keepAlive.set(true);
            	rrqMod.performDelayPacketModification(req, recvPort, delayedPacketConsumer);
            } else if (rrqMod.getDuplicatePacketModification() != null) {
            	keepAlive.set(true);
            	rrqMod.performDuplicatePacketModification(req, recvPort, delayedPacketConsumer);;
            }
          }
        } else if (req.type() == RequestType.WRITE) {
        	WriteRequestModification wrqMod = packetModifier.getWrqModification();
        	
          if (packetModifier.getWrqModification() != null) {
            if (wrqMod != null) {
            	int recvPort = Configuration.get().INTERMEDIATE_PORT;
            	
            	if (wrqMod.getDelayModification() != null) {
            		keepAlive.set(true);
            		wrqMod.performDelayPacketModification(req, recvPort, delayedPacketConsumer);
            	} else if (wrqMod.getDuplicatePacketModification() != null) {
            		keepAlive.set(true);
            		wrqMod.performDelayPacketModification(req, recvPort, delayedPacketConsumer);
            	}           	
            }
          }
        }
      }

      /*
       * END of DISGUSTINGNESS
       */
      
      
      Runnable tftpTransfer = new TftpTransfer(requestDatagram, packetModifier);
      Thread t = new Thread(tftpTransfer, "#" + (connectionThreads.size() + 1));
      connectionThreads.add(t);
      t.start();
      hasRun = true;
    } while (!hasRun || keepAlive.get() || connectionThreads.stream().anyMatch((t) -> t.isAlive()));
    
    clientSocket.close();
    log("All connections terminated");
  }
  
  private void log(String msg) {
    System.out.println("[INTERMEDIATE] " + msg);
  }
}
