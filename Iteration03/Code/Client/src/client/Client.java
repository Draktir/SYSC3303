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
import packet.ErrorPacket.ErrorCode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.FileAlreadyExistsException;
import java.util.Scanner;

import Configuration.Configuration;

import java.io.File;

import file_io.FileReader;
import file_io.FileWriter;

public class Client {
  final double MAX_FILE_SIZE = 512 * (Math.pow(2, 16) - 1); // 2 byte range for block numbers, less block number 0
  private ServerConnection serverConnection;
  private PacketParser packetParser = new PacketParser();
  private FileReader fileReader;
  private FileWriter fileWriter;
  private boolean transferComplete = false;
  private DatagramPacket serverLastSent = null;

  /**
   * Default Client constructor, instantiates the server Connection
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
    new Client().start();
  }

  public void start() {
    Scanner scan = new Scanner(System.in);
    int command;
    String fileName = "";

    do {
      System.out.println("TFTP Client");
      System.out.println("  [ 1 ] Write file to server");
      System.out.println("  [ 2 ] Read file from server");
      System.out.println("  [ 0 ] Exit");
      System.out.print(" > ");

      command = scan.nextInt();

      switch (command) {
      case 1:
        do {
          System.out.print("Please enter a file name: ");
          fileName = scan.next();
        } while (!this.validateFilename(fileName));
        this.sendFileToServer(fileName, "netAsCiI");
        serverConnection.resetTid(); // reset for next connection
        break;

      case 2:
        do {
          System.out.print("Please enter a file name: ");
          fileName = scan.next();
        } while (fileName == null || fileName.length() == 0);
        this.downloadFileFromServer(fileName, "ocTeT");
        serverConnection.resetTid(); // reset for next connection
        break;
      }
    } while (command != 0);

    scan.close();    
  }
  
  public boolean validateFilename(String filename) {
    File f = new File(filename);

    if (!f.exists()) {
      System.out.println("The file does not exist.");
      return false;
    }
    if (f.isDirectory()) {
      System.out.println("The filename you entered is a directory.");
      return false;
    }
    if (f.length() > MAX_FILE_SIZE) {
      System.out.println("The file is too big. size: " + f.length() + " max: " + MAX_FILE_SIZE);
      return false;
    }
    if (f.length() < 1) {
      System.out.println("The file is empty. Cannot send an empty file.");
      return false;
    }

    return true;
  }

  /**
   * Initiates sending a file to the server (WriteRequest)
   * 
   * @param filename
   * @param mode
   */
  private void sendFileToServer(String filename, String mode) {
    transferComplete = false;

    log("opening " + filename + " for reading");
    // open file for reading
    try {
      fileReader = new FileReader(filename);
    } catch (FileNotFoundException e1) {
      log("ERROR: The file you're trying to send could not be found.");
      return;
    }

    InetAddress remoteHost;
    try {
      remoteHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      return;
    }

    WriteRequest request = new RequestBuilder()
        .setRemoteHost(remoteHost)
        .setRemotePort(Configuration.INTERMEDIATE_PORT)
        .setFilename(filename)
        .setMode(mode)
        .buildWriteRequest();

    log("Sending Write Request to server on port " + Configuration.INTERMEDIATE_PORT);
    log("Expecting ACK with block #0");
    
    DatagramPacket recvdDatagram = serverConnection.sendPacketAndReceive(request);
    
    // remember the server's TID
    serverConnection.setServerAddress(recvdDatagram.getAddress());
    serverConnection.setServerPort(recvdDatagram.getPort());

    log("Packet received.");
    printPacketInformation(recvdDatagram);
    
    int blockNumber = 0;

    do {
      if (!isDuplicatePacket(recvdDatagram)) {
    	  Acknowledgement ack = null;
          try {
            ack = packetParser.parseAcknowledgement(recvdDatagram);
          } catch (InvalidAcknowledgementException e) {
            String errMsg = "Not a valid ACK: " + e.getMessage();
            log(errMsg);
            handleParseError(errMsg, recvdDatagram);
            return;
          }

          if (ack.getBlockNumber() != blockNumber) {
            String errMsg = "ACK block #" + ack.getBlockNumber() + " is wrong, expected block #" + blockNumber;
            log(errMsg);
            sendErrorPacket(errMsg, recvdDatagram);
            return;
          }

          log("Received a valid ACK:\n" + ack.toString() + "\n");
          
          // store last received datagram to test for duplication
          serverLastSent = recvdDatagram;
          
          // now sending next block
          blockNumber++;
          
          log("Reading block #" + blockNumber + " from file");
          
          byte[] buffer = new byte[512];
          int bytesRead = fileReader.readNextBlock(buffer);

          if (bytesRead < 0) {
            log("ERROR: Could not read from the file: " + fileReader.getFilename());
            return;
          }
          
          byte[] fileData = new byte[bytesRead];
          System.arraycopy(buffer, 0, fileData, 0, bytesRead);

          DataPacket dataPacket = new DataPacketBuilder()
              .setRemoteHost(ack.getRemoteHost())
              .setRemotePort(ack.getRemotePort())
              .setBlockNumber(ack.getBlockNumber() + 1)
              .setFileData(fileData)
              .buildDataPacket();
          
          log("Sending Data Packet to server: \n" + dataPacket.toString() + "\n");

          // Check if we have read the whole file
          if (fileData.length < 512) {
            log("Sending last data packet.");
            fileReader.close();

            // send the last data packet
            DatagramPacket responseDatagram = serverConnection.sendPacketAndReceive(dataPacket);
            
            try {
              packetParser.parse(responseDatagram);
            } catch (InvalidPacketException e) {
              String errMsg = "Invalid ACK: " + e.getMessage();
              log(errMsg);
              handleParseError(errMsg, responseDatagram);
              return;
            }
            
            // we're gonna terminate the connection either way in this iteration
            transferComplete = true;
            break;
          }

          // send the data packet
          recvdDatagram = serverConnection.sendPacketAndReceive(dataPacket);
      }
      else {
    	  // TODO: handle duplicate ACK received
    	  log("Duplicate ACK received from the server.");
    	  
      }
      
    } while (!transferComplete);

    log("File transfer successful.");
  }

  /**
   * Initiates downloading a file from the server (Read Request)
   * 
   * @param filename
   * @param mode
   */
  public void downloadFileFromServer(String filename, String mode) {
    transferComplete = false;
    fileWriter = null;
    
    InetAddress remoteHost;
    try {
      remoteHost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      e.printStackTrace();
      return;
    }

    ReadRequest request = new RequestBuilder()
        .setRemoteHost(remoteHost)
        .setRemotePort(Configuration.INTERMEDIATE_PORT)
        .setFilename(filename)
        .setMode(mode)
        .buildReadRequest();

    log("Sending Read Request to server.");
    log("Expecting Data Packet with block #1");
    
    DatagramPacket recvdDatagram = serverConnection.sendPacketAndReceive(request);

    // remember the server's TID
    serverConnection.setServerAddress(recvdDatagram.getAddress());
    serverConnection.setServerPort(recvdDatagram.getPort());
    
    int blockNumber = 1;

    do {
      DataPacket dataPacket = null;
      try {
        dataPacket = packetParser.parseDataPacket(recvdDatagram);
      } catch (InvalidDataPacketException e) {
        String errMsg = "Not a valid Data Packet: " + e.getMessage();
        log(errMsg);
        handleParseError(errMsg, recvdDatagram);
        break;
      }

      if (dataPacket.getBlockNumber() != blockNumber) {
        String errMsg = "Data packet has the wrong block#, expected block #" + blockNumber;
        log(errMsg);
        sendErrorPacket(errMsg, recvdDatagram);
        break;
      }
      
      log("Received valid Data packet:\n" + dataPacket.toString() + "\n");
      log("\tWriting file block# " + blockNumber);
      
      // creating file if not yet exists
      if (fileWriter == null) {
        log("Creating file " + filename + " for writing.");
        // create file for reading
        try {
          fileWriter = new FileWriter(filename);
        } catch (FileNotFoundException e1) {
          e1.printStackTrace();
          log("ERROR: Could not create new file for downloading.");
          break;
        } catch (FileAlreadyExistsException e) {
          log("ERROR: " + filename + " already exists on this machine.");
          e.printStackTrace();
          break;
        } catch (IOException e) {
          log("ERROR: " + e.getMessage());
          e.printStackTrace();
          break;
        }
      }

      
      byte[] fileData = dataPacket.getFileData();
      try {
        fileWriter.writeBlock(fileData);
      } catch (IOException e) {
        log(e.getMessage());
        e.printStackTrace();
        break;
      }

      Acknowledgement ack = new AcknowledgementBuilder()
          .setRemoteHost(dataPacket.getRemoteHost())
          .setRemotePort(dataPacket.getRemotePort())
          .setBlockNumber(dataPacket.getBlockNumber())
          .buildAcknowledgement();
      
      log("Sending ACK:\n" + ack.toString() + "\n");

      // Check for the last data packet
      if (fileData.length < 512) {
        log("\tAcknowledging last data packet.");
        transferComplete = true;
        // send the last ACK
        serverConnection.sendPacket(ack);
        break;
      }

      recvdDatagram = serverConnection.sendPacketAndReceive(ack);
      blockNumber++;      
    } while (!transferComplete);

    log("File transfer ended.");
    if (fileWriter != null) {
      fileWriter.close();
    }
  }

  private void handleParseError(String message, DatagramPacket packet) {
    // first figure out whether datagram is an Error packet
    ErrorPacket errPacket = null;
    try {
      errPacket = packetParser.parseErrorPacket(packet);
    } catch (InvalidErrorPacketException e) {
      // Nope, not an error packet, the client screwed up, send him an error
      log("Invalid packet received. Sending error packet to server.\n");
      sendErrorPacket(message, packet);
      return;
    }

    // yes, we got an error packet, so we (the server) screwed up.
    log("Received an error packet: " + errPacket.getErrorCode() + "\n" + errPacket.toString() + "\n");
    log("");
    log("received ERROR " + errPacket.getErrorCode() + ": Server says\n'" + errPacket.getMessage() + "'\n");
  }
  
  private boolean isDuplicatePacket(DatagramPacket packet) {
	if (serverLastSent == packet) return true;
	return false;
  }
  
  private void sendErrorPacket(String message, DatagramPacket packet) {
    ErrorPacket errPacket = new ErrorPacketBuilder()
        .setRemoteHost(packet.getAddress())
        .setRemotePort(packet.getPort())
        .setErrorCode(ErrorCode.ILLEGAL_TFTP_OPERATION)
        .setMessage(message)
        .buildErrorPacket();
    
    log("Sending error packet to server:\n" + errPacket.toString() + "\n");
    serverConnection.sendPacket(errPacket);
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

  private void log(String message) {
    System.out.println("[CLIENT] " + message);
  }
}
