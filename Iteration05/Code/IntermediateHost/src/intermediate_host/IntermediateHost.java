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
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
import packet.ReadRequest;
import packet.Request;
import packet.Request.RequestType;
import packet.WriteRequest;
import utils.PacketPrinter;
import utils.UserIpInput;
import utils.Logger;







/*
 * TODO: 
 *  - What about TID modification of RRQ/WRQ?
 *  - Delaying RRQ or WRQ always happens, not just for the first one
 *    - probably happens again in TftpTransfer class
 */






public class IntermediateHost {
  private DatagramSocket clientSocket;
  private PacketModifier packetModifier;
  Logger log = new Logger("IntermediateHost");

  /**
   * Main method which creates an instance of IntermediateHost to forward and
   * receive TFTP requests.
   * 
   * @param args
   */
  public static void main(String[] args) {
    IntermediateHost h = new IntermediateHost();
    Scanner scan = new Scanner(System.in);
    
    InetAddress serverAddress = null;
		try {
			serverAddress = UserIpInput.get(scan);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
    
    do {
      h.go(serverAddress);
      System.out.println("\nSimulation complete!\n");
      System.out.print("Do you want to simulate another error scenario? (y/n) ");
    } while (scan.next().equalsIgnoreCase("y"));
    scan.close();
  }

  public void go(InetAddress serverAddress) {
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

    log.logAlways("Waiting for client requests on port " + Configuration.get().INTERMEDIATE_PORT);

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
      
      log.logAlways("Received packet");
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
    	log.logAlways(req.toString());
      	
      	/* 
      	 * consumer that handles a delayed packet by starting a new TftpTransfer thread with
      	 * a duplicated DatagramPacket
      	 */
        Consumer<Packet> delayedPacketConsumer = (p) -> {
      		byte[] data = p.getPacketData();
          DatagramPacket dp = new DatagramPacket(data, data.length, 
              p.getRemoteHost(), p.getRemotePort());
          Runnable tftpTransfer = new TftpTransfer(dp, serverAddress, packetModifier);
          Thread t = new Thread(tftpTransfer, "#" + (connectionThreads.size() + 1));
          connectionThreads.add(t);
          t.start();
          keepAlive.set(false);
      	};
      	
      	if (req.type() == RequestType.READ) {
      		ReadRequestModification rrqMod = packetModifier.getRrqModification();
      		
      		// increase RRQ count by one
      		packetModifier.setRrqCount(packetModifier.getRrqCount() + 1);
      		
      		if (rrqMod != null) {
          	int recvPort = Configuration.get().INTERMEDIATE_PORT;
          	
            if (rrqMod.getDelayModification() != null && packetModifier.getRrqCount() == rrqMod.getPacketNumber()) {
            	keepAlive.set(true);
            	rrqMod.performDelayPacketModification((ReadRequest) req, recvPort, delayedPacketConsumer);
            	continue;
            } else if (rrqMod.getDuplicatePacketModification() != null && packetModifier.getRrqCount() == rrqMod.getPacketNumber()) {
            	keepAlive.set(true);
            	rrqMod.performDuplicatePacketModification((ReadRequest) req, recvPort, delayedPacketConsumer);
            } else if (rrqMod.getDropModification() != null && packetModifier.getRrqCount() == rrqMod.getPacketNumber()) {
          	  // nasty, nasty hack
              packetModifier.setRrqCount(packetModifier.getRrqCount() - 1);
              byte[] d = packetModifier.process((ReadRequest) req, recvPort, serverAddress, Configuration.get().SERVER_PORT, (p) -> {});
            	if (d == null) {
            		continue;
            	}
            } else {
              packetModifier.setRrqCount(packetModifier.getRrqCount() - 1);
            }
          }
        } else if (req.type() == RequestType.WRITE) {
        	WriteRequestModification wrqMod = packetModifier.getWrqModification();
        	
        	// increase WRQ count by one
        	packetModifier.setWrqCount(packetModifier.getWrqCount() + 1);

          if (wrqMod != null) {
          	int recvPort = Configuration.get().INTERMEDIATE_PORT;
          	
          	if (wrqMod.getDelayModification() != null && packetModifier.getWrqCount() == wrqMod.getPacketNumber()) {
          		keepAlive.set(true);
          		wrqMod.performDelayPacketModification((WriteRequest) req, recvPort, delayedPacketConsumer);
          		continue;
          	} else if (wrqMod.getDuplicatePacketModification() != null && packetModifier.getWrqCount() == wrqMod.getPacketNumber()) {
          		keepAlive.set(true);
          		wrqMod.performDuplicatePacketModification((WriteRequest) req, recvPort, delayedPacketConsumer);
          	} else if (wrqMod.getDropModification() != null && packetModifier.getWrqCount() == wrqMod.getPacketNumber()) {
        		  // nasty, nasty hack
          	  packetModifier.setWrqCount(packetModifier.getWrqCount() - 1);
          	  byte[] d = packetModifier.process((WriteRequest) req, recvPort, serverAddress, Configuration.get().SERVER_PORT, (p) -> {});
          		if (d == null) {
            		continue;
            	}
          	} else {
          	  packetModifier.setWrqCount(packetModifier.getWrqCount() - 1);
          	}
          }
        }
      }

      /*
       * END of DISGUSTINGNESS
       */
      
      
      Runnable tftpTransfer = new TftpTransfer(requestDatagram, serverAddress, packetModifier);
      Thread t = new Thread(tftpTransfer, "#" + (connectionThreads.size() + 1));
      connectionThreads.add(t);
      t.start();
      hasRun = true;
    } while (!hasRun || keepAlive.get() || connectionThreads.stream().anyMatch((t) -> t.isAlive()));
    
    clientSocket.close();
    log.logAlways("All connections terminated");
  }
}
