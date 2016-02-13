package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.nio.file.FileAlreadyExistsException;

import file_io.FileReader;
import file_io.FileWriter;
import packet.Acknowledgement;
import packet.AcknowledgementBuilder;
import packet.DataPacket;
import packet.DataPacketBuilder;
import packet.ErrorPacket;
import packet.ErrorPacketBuilder;
import packet.InvalidAcknowledgementException;
import packet.InvalidDataPacketException;
import packet.InvalidErrorPacketException;
import packet.InvalidRequestException;
import packet.PacketParser;
import packet.ReadRequest;
import packet.Request;
import packet.WriteRequest;
import packet.ErrorPacket.ErrorCode;
import packet.Request.RequestType;

/**
 * The RequestHandler class handles requests received by Listener.
 * It creates a separate thread (needs testing) for each Client 
 * request that is received.
 * 
 * @author Loktin Wong
 * @author Philip Klostermann
 * @version 1.0.1
 * @since 25-01-2016
 */
class RequestHandler implements Runnable {
  private DatagramPacket requestPacket;
  private ClientConnection clientConnection;
  private PacketParser packetParser = new PacketParser();
  
  /**
   * Default RequestHandler constructor instantiates requestPacekt to
   * the packet passed down from the Listener class.
   * 
   * @param packet
   */
  public RequestHandler(DatagramPacket requestPacket) {
    this.requestPacket = requestPacket;
  }
  
  /**
   * Processes the received request and initiates file transfer.
   */
  public void run() {
    try {
      this.clientConnection = new ClientConnection(requestPacket);
    } catch (SocketException e) {
      e.printStackTrace();
      System.err.println("Could not create client socket.");
      return;
    }
    
    Request request = null;
    
    System.out.println("---------------------------------------------------------");
    log("Incoming request");
    printPacketInformation(requestPacket);
    
    // 1. parse the orignal client request
    try {
      request = packetParser.parseRequest(requestPacket);
    } catch (InvalidRequestException e) {
      String errMsg = "Invalid request: " + e.getMessage();
      log(errMsg);
      handleParseError(errMsg, requestPacket);
      log("Terminating this connection thread");
      return;
    }
    
    // 2. determine the type of transfer
    if (request.type() == RequestType.READ) {
      // initiate ReadRequest
      log("Received ReadRequest, initiating file transfer");
      sendFileToClient((ReadRequest) request);
    } else if (request.type() == RequestType.WRITE) {
      // initiate WriteRequest
      log("Received WriteRequest, initiating file transfer");
      receiveFileFromClient((WriteRequest) request);
    } else {
      // should never really get here
      log("Could not identify request type, but it was parsed.");
      handleParseError("Invalid request. Expected RRQ or WRQ.", requestPacket);
    }
    
    log("Terminating thread");
  }
  
  private void sendFileToClient(ReadRequest request) {
    log("opening " + request.getFilename() + " for reading.");
    
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(request.getFilename());
    } catch (FileNotFoundException e) {
      log("opening " + request.getFilename() + " for reading failed. File not found!");
      // TODO Send an Error (FILE_NOT_FOUND)
      e.printStackTrace();
      return;
    }
    
    // check file size before sending
    File f = new File(request.getFilename());
    final long MAX_FILE_SIZE = (long) ((Math.pow(2, 16) - 1) * 512);
    if (f.length() > MAX_FILE_SIZE) {
      log("The requested file is too big: " + f.length() + " bytes. Max is " + MAX_FILE_SIZE + " bytes.");
      return;
    }
    
    int blockNumber = 1;
    int bytesRead;
    byte[] fileBuffer = new byte[512];
        
    do {
      // 1. read the next block from the file 
      log("reading block #" + blockNumber + " from file.");
      bytesRead = fileReader.readNextBlock(fileBuffer);
      
      // 2. copy file data out of the buffer
      byte[] fileData = new byte[bytesRead];
      System.arraycopy(fileBuffer, 0, fileData, 0, bytesRead);
      
      // 3. build data packet
      DataPacket dataPacket = new DataPacketBuilder()
          .setRemoteHost(request.getRemoteHost())
          .setRemotePort(request.getRemotePort())
          .setBlockNumber(blockNumber)
          .setFileData(fileData)
          .buildDataPacket();
      
      log("sending data packet, block #" + dataPacket.getBlockNumber());
      log("expecting ACK, block #" + blockNumber);
      
      // 4. send data packet and wait for response
      DatagramPacket responseDatagram;
      responseDatagram = clientConnection.sendPacketAndReceive(dataPacket);
      
      if (responseDatagram == null) {
        // TODO: handle timeouts
        log("Did not receive a response from the client.");
        return;
      }
      
      log("Received packet.");
      printPacketInformation(responseDatagram);
      
      // 5. parse client response
      Acknowledgement ack = null;
      try {
       ack = packetParser.parseAcknowledgement(responseDatagram);
      } catch (InvalidAcknowledgementException e) {
        String errMsg = "Not a valid ACK: " + e.getMessage();
        log(errMsg);
        handleParseError(errMsg, responseDatagram);
        return;
      }
       
      // make sure the block number is correct
      if (ack.getBlockNumber() != blockNumber) {
        String errMsg = "ACK has the wrong block#, got #" + ack.getBlockNumber() + "expected #" + blockNumber;
        log(errMsg);
        sendErrorPacket(errMsg, responseDatagram);
        return;
      }
      
      log("Received valid ACK packet\n" + ack.toString() + "\n");
      
      // 6. repeat
      blockNumber++;
    } while (bytesRead == 512);
    
    fileReader.close();
    
    log("File " + request.getFilename() +" successfully sent to client in " + (blockNumber - 1) + " blocks.");
  }
  
  private void receiveFileFromClient(WriteRequest request) {
    log("Opening " + request.getFilename() + " for writing.");
    FileWriter fileWriter = null;
  
    try {
      fileWriter = new FileWriter(request.getFilename());
    } catch (FileAlreadyExistsException e) {
      // TODO Send error FILE_ALREADY_EXISTS
      log("opening " + request.getFilename() + " for writing failed." + " File already exists!");
      e.printStackTrace();
      return;
    } catch (IOException e) {
      // TODO Send error DISK_FULL_OR_ALLOCATION_EXCEEDED
      log("opening " + request.getFilename() + " for writing failed." + " Disk is full!");
      e.printStackTrace();
      return;
    }

    int blockNumber = 0;
    
    while (true) {
      //1. build an ACK
      log("Sending ACK with block#" + blockNumber);
      
      Acknowledgement ack = new AcknowledgementBuilder()
          .setRemoteHost(request.getRemoteHost())
          .setRemotePort(request.getRemotePort())
          .setBlockNumber(blockNumber)
          .buildAcknowledgement();
      
      //2. send ACK and wait for a data packet
      DatagramPacket receivedDatagram = clientConnection.sendPacketAndReceive(ack);
      
      if (receivedDatagram == null) {
        // TODO: handle timeouts
        log("Did not receive a data packet from the client.");
        return;
      }
      
      // we are now expecting the next sequential block number
      blockNumber++;

      log("Received packet from client, parsing...");
      printPacketInformation(receivedDatagram);
      
      DataPacket dataPacket;
      try {
        dataPacket = packetParser.parseDataPacket(receivedDatagram);
      } catch (InvalidDataPacketException e) {
        String errMsg = "Not a valid Data Packet: " + e.getMessage();
        log(errMsg);
        handleParseError(errMsg, receivedDatagram);
        return;
      }
      
      // make sure the block number is correct
      if (dataPacket.getBlockNumber() != blockNumber) {
        String errMsg = "Data packet has the wrong block#, got #" + dataPacket.getBlockNumber() + "expected #" + blockNumber;
        log(errMsg);
        sendErrorPacket(errMsg, receivedDatagram);
        return;
      }
      
      log("Received valid Data packet:\n" + dataPacket.toString() + "\n");
      log("Writing data to disk");
      
      //3. write file data to disk
      try {
        fileWriter.writeBlock(dataPacket.getFileData());
      } catch (IOException e) {
    	System.err.println("Disk is full or allocation exceeded");  
        // TODO send an DISK_FULL_OR_ALLOCATION_EXCEEDED error
        e.printStackTrace();
        return;
      }
      
      // was this the last data packet?
      if (dataPacket.getFileData().length < 512) {
        log("Received the last data packet");
        // send last ACK
        Acknowledgement lastAck = new AcknowledgementBuilder()
            .setRemoteHost(request.getRemoteHost())
            .setRemotePort(request.getRemotePort())
            .setBlockNumber(blockNumber)
            .buildAcknowledgement();
    
        log("Sending final ACK with block #" + blockNumber);
        clientConnection.sendPacket(lastAck);
        // we're done
        break;
      }
      
      log("");
    }
    
    fileWriter.close();
    
    log("File " + request.getFilename() + " successfully received from client in " 
          + blockNumber + " blocks.");
  }
  
  private void handleParseError(String message, DatagramPacket datagram) {
    // first figure out whether datagram is an Error packet
    ErrorPacket errPacket = null;
    try {
      errPacket = packetParser.parseErrorPacket(datagram);
    } catch (InvalidErrorPacketException e) {
      // Nope, not an error packet, the client screwed up, send him an error
      log("Invalid packet received. Sending error packet to client.\n");
      sendErrorPacket(message, datagram);
      return;
    }

    // yes, we got an error packet, so we (the server) screwed up.
    log("Received an error packet: " + errPacket.getErrorCode() + "\n" + errPacket.toString() + "\n");
    log("");
    log("received ERROR " + errPacket.getErrorCode() + ": Client says\n'" + errPacket.getMessage() + "'\n");
  }
  
  private void sendErrorPacket(String message, DatagramPacket requestPacket) {
    ErrorPacket errPacket = new ErrorPacketBuilder()
        .setRemoteHost(requestPacket.getAddress())
        .setRemotePort(requestPacket.getPort())
        .setErrorCode(ErrorCode.ILLEGAL_TFTP_OPERATION)
        .setMessage(message)
        .buildErrorPacket();
    
    log("Sending error to client:\n" + errPacket.toString());
    
    clientConnection.sendPacket(errPacket);
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
    String name = Thread.currentThread().getName();
    System.out.println("[RequestHandler] " + name + ": " + msg);
  }
}