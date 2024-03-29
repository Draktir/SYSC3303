/**
 * The Client class implements an application that will
 * send and receive TFTP requests to and from a server
 * 
 * @author  Loktin Wong
 * @author  Philip Klostermann
 * @version 1.0.0
 * @since 22-01-2016
 */

// TODO: add proper documentation to all new functions, and make changes to existing documentation.

package client;

import packet.*;
import client.FileReader;
import client.FileWriter;
import Configuration.*;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {
  //public static final int SERVER_PORT = 68;
  private ServerConnection serverConnection;
  private FileReader fileReader;
  private FileWriter fileWriter;
  private boolean transferComplete = false;

  /**
   * Default Client constructor which instantiates the server Connection
   */
  public Client() {
    try {
      serverConnection = new ServerConnection();
    } catch (SocketException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Main method which creates an instance of Client.
   * 
   * @param args
   */
  public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    Client c = new Client();
    int command;
	
  	do {
  		System.out.println("TFTP Client");
	    System.out.println("  [ 1 ] Write file to server");
	    System.out.println("  [ 2 ] Read file from server");
	    System.out.println("  [ 0 ] Exit");
	    System.out.print(" > ");
	    
	    command = sc.nextInt();
	    
	    switch (command) {
	      case 1:
	    	  c.sendFileToServer("testWriteFile.txt", "netAsCiI");
	    	  System.out.println("[SYSTEM] Uploaded file testWriteFile.txt to server.");
	    	  System.out.println("[SYSTEM] Please restart the IntermediatHost now."); // TODO: remove this
	    	  break;
	      case 2:
	    	  c.downloadFileFromServer("testReadFile.txt", "ocTeT");
	    	  System.out.println("[SYSTEM] Downloaded file testReadFile.txt from server.");
	    	  System.out.println("[SYSTEM] Please restart the IntermediatHost now."); // TODO: remove this
	    	  break;
	    }
	  } while (command != 0);
	
	  sc.close();
	}

  
  /**
   * Initiates sending a file to the server (WriteRequest)
   * 
   * @param filename
   * @param mode
   */
  private void sendFileToServer(String filename, String mode) {
    transferComplete = false;
    // open file for reading
    try {
      fileReader = new FileReader(filename);
    } catch (FileNotFoundException e1) {
      System.err.println("The file you're trying to send could not be fonud on this computer.");
      return;
    }

    InetAddress remoteHost;
    try {
      remoteHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      return;
    }
    
    WriteRequest req = new RequestBuilder()
        .setRemoteHost(remoteHost)
        .setRemotePort(Configuration.client_SERVER_PORT)
        .setFilename(filename)
        .setMode(mode)
        .buildWriteRequest();
    
    performFileTransfer(req);
  }

  /**
   * Initiates downloading a file from the server (Read Request)
   * 
   * @param filename
   * @param mode
   */  
  public void downloadFileFromServer(String filename, String mode) {
    transferComplete = false;
    // create file for reading
    try {
      fileWriter = new FileWriter(filename);
    } catch (FileNotFoundException e1) {
      e1.printStackTrace();
      System.err.println("Could not create new file for downloading.");
      return;
    }
   
    InetAddress remoteHost;
    try {
      remoteHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      return;
    }
    
    ReadRequest req = new RequestBuilder()
        .setRemoteHost(remoteHost)
        .setRemotePort(Configuration.client_SERVER_PORT)
        .setFilename(filename)
        .setMode(mode)
        .buildReadRequest();
    
    performFileTransfer(req);
  }

  /**
   * Performs the actual file transfer, sending requests and receiving responses
   * 
   * @param request
   */
  private void performFileTransfer(Packet request) {
    PacketParser parser = new PacketParser();
    Packet recvdPacket = serverConnection.sendPacketAndReceive(request);
    
    do {
      Packet response;
      try {
        response = parser.parse(recvdPacket);
      } catch (InvalidPacketException e) {
        e.printStackTrace();
        break;
      }
      
      if (response instanceof Acknowledgement) {
        recvdPacket = handleAcknowledgement((Acknowledgement) response);
      } else if (response instanceof DataPacket) {
        recvdPacket = handleDataPacket((DataPacket) response);
      } else {
        System.err.println("Invalid packet received");
        break;
      }
      
      //System.out.println("\n\tPacket received from Server");
      
      if (recvdPacket != null) {
        printPacketInformation(recvdPacket);
      } else {
        System.out.println("[SYSTEM] File successfully transferred.");
      }
      
    } while (!transferComplete);

    System.out.println("[SYSTEM] File transfer ended."); // TODO: this doesn't really need to be here.
  }

  /**
   * Handles an incoming acknowledgement by sending the next file block
   * 
   * @param ack
   * @return
   */
  private Packet handleAcknowledgement(Acknowledgement ack) {
    System.out.println("\n\tACK received, block #" + ack.getBlockNumber());
    
    byte[] buffer = new byte[512];
    int bytesRead = fileReader.readBlock(buffer);

    byte[] fileData = new byte[bytesRead];
    System.arraycopy(buffer, 0, fileData, 0, bytesRead);

    System.out.println("\n\tFile data length (bytes): " + bytesRead);
    
    DataPacket dataPacket = new DataPacketBuilder()
        .setRemoteHost(ack.getRemoteHost())
        .setRemotePort(ack.getRemotePort())
        .setBlockNumber(ack.getBlockNumber() + 1)
        .setFileData(fileData)
        .buildDataPacket();

    printPacketInformation(dataPacket);

    // Check if we have read the whole file
    if (fileData.length < 512) {
      System.out.println("[SYSTEM] Sending last data packet.");
      transferComplete = true;
      fileReader.close();
      
      // send the last data packet
      serverConnection.sendPacketAndReceive(dataPacket);

      // TODO: We should make sure we get an ACK and resend the last data packet
      // if it failed. Not needed for this iteration though.
      return null;
    }

    return serverConnection.sendPacketAndReceive(dataPacket);
  }

  /**
   * Handles an incoming data packet by responding with an ACK
   *  
   * @param dataPacket
   * @return
   */
  private Packet handleDataPacket(DataPacket dataPacket) {
    System.out.println("\tDATA received, block #" + dataPacket .getBlockNumber());
    
    System.out.println("\tWriting file block# " + dataPacket.getBlockNumber());
    byte[] fileData = dataPacket.getFileData();
    fileWriter.writeBlock(fileData);
    
    Acknowledgement ack = new AcknowledgementBuilder()
            .setRemoteHost(dataPacket.getRemoteHost())
            .setRemotePort(dataPacket.getRemotePort())
            .setBlockNumber(dataPacket.getBlockNumber())
            .buildAcknowledgement();
    
    printPacketInformation(ack);
    
    // Check for the last data packet
    if (fileData.length < 512) {
      System.out.println("\tAcknowledging last data packet.");
      transferComplete = true;
      fileWriter.close();
      // send the last ACK
      serverConnection.sendPacket(ack);
      return null;
    }
    
    return serverConnection.sendPacketAndReceive(ack);
  }

  /**
   * Prints out request contents as a String and in bytes.
   * 
   * @param buffer
   */
  public void printPacketInformation(Packet packet) {
    byte[] data = packet.getPacketData();
    String contents = new String(data); // TODO: format to handle \n in string. (substrings)

    System.out.println("\n\tPacket contents: ");
    System.out.println("\t" + contents);

    System.out.println("\tPacket contents (bytes): ");
    System.out.print("\t");
    for (int i = 0; i < data.length; i++) {
      System.out.print(data[i] + " ");
    }
    System.out.println();
  }
}
