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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Scanner;
import java.io.File;

import file_io.FileReader;
import file_io.FileWriter;

public class Client {
  public static final int SERVER_PORT = 68;
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
	
    String file_name = null;
    System.out.println("please enter the file name: \n");
    file_name = sc.next( );
	File f = new File(file_name);
	while(!(f.exists() || f.isDirectory())) { 
		System.out.println("please enter a valid and existing filename: \n");
		file_name = sc.next( );
	}
	
  	do {
  		System.out.println("TFTP Client");
	    System.out.println("  [ 1 ] Write file to server");
	    System.out.println("  [ 2 ] Read file from server");
	    System.out.println("  [ 0 ] Exit");
	    System.out.print(" > ");
	    
	    command = sc.nextInt();
	    
	    switch (command) {
	      case 1:
	    	  c.sendFileToServer(file_name, "netAsCiI");
	    	  System.out.println("[SYSTEM] Uploaded file testWriteFile.txt to server.");
	    	  System.out.println("[SYSTEM] Please restart the IntermediatHost now."); // TODO: remove this
	    	  break;
	      case 2:
	    	  c.downloadFileFromServer(file_name, "ocTeT");
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
        .setRemotePort(SERVER_PORT)
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
    } catch (FileAlreadyExistsException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
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
        .setRemotePort(SERVER_PORT)
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
    DatagramPacket recvdDatagram = serverConnection.sendPacketAndReceive(request);

    do {
      Packet response;
      try {
        response = parser.parse(recvdDatagram);
      } catch (InvalidPacketException e) {
        e.printStackTrace();
        break;
      }
      
      if (response instanceof Acknowledgement) {
        recvdDatagram = handleAcknowledgement((Acknowledgement) response);
      } else if (response instanceof DataPacket) {
        recvdDatagram = handleDataPacket((DataPacket) response);
      } else {
        System.err.println("Invalid packet received");
        break;
      }
      
      //System.out.println("\n\tPacket received from Server");
      
      if (recvdDatagram != null) {
        //printPacketInformation(recvdDatagram);
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
  private DatagramPacket handleAcknowledgement(Acknowledgement ack) {
    System.out.println("\n\tACK received, block #" + ack.getBlockNumber());
    
    byte[] buffer = new byte[512];
    int bytesRead = fileReader.readNextBlock(buffer);

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
  private DatagramPacket handleDataPacket(DataPacket dataPacket) {
    System.out.println("\tDATA received, block #" + dataPacket .getBlockNumber());
    
    System.out.println("\tWriting file block# " + dataPacket.getBlockNumber());
    byte[] fileData = dataPacket.getFileData();
    try {
      fileWriter.writeBlock(fileData);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
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
