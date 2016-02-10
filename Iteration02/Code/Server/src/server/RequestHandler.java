package server;

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
import packet.InvalidRequestException;
import packet.Packet;
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
   * 
   */
  public void run() {
    try {
      this.clientConnection = new ClientConnection(requestPacket);
    } catch (SocketException e) {
      e.printStackTrace();
      System.err.println("Could not create client socket.");
      return;
    }
    
    PacketParser parser = new PacketParser();
    Request request = null;
    
    // 1. parse the orignal client request
    try {
      request = parser.parseRequest(requestPacket);
    } catch (InvalidRequestException e) {
      String errMsg = "Invalid request: " + e.getMessage();
      System.err.println(errMsg);
      handlePacketError(errMsg, requestPacket);
      return;
    }
    
    // 2. determine the type of transfer
    if (request.type() == RequestType.READ) {
      // initiate ReadRequest
      sendFileToClient((ReadRequest) request);
    } else if (request.type() == RequestType.WRITE) {
      // initiate WriteRequest
      receiveFileFromClient((WriteRequest) request);
    } else {
      // should never really get here
      System.err.println("Could not identify request type, but it was parsed. SOMETHING IS SERIOUSLY WRONG!");
      System.err.println(request);
      handlePacketError("Invalid request. Expected RRQ or WRQ.", requestPacket);
    }
    
    System.out.println("Terminated thread " + Thread.currentThread().getName());
  }
  
  private void sendFileToClient(ReadRequest request) {
    System.out.println("[SYSTEM] opening " + request.getFilename() + " for reading.");
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(request.getFilename());
    } catch (FileNotFoundException e) {
      // TODO Send an Error (FILE_NOT_FOUND)
      e.printStackTrace();
      return;
    }
    
    int blockNumber = 1;
    int bytesRead;
    byte[] fileBuffer = new byte[512];
    PacketParser packetParser = new PacketParser();
    
    do {
      // 1. read the next block from the file 
      System.out.println("[SYSTEM] Reading block #" + blockNumber + " from file.");
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
      
      printPacketInformation(dataPacket);
      
      // 4. send data packet and wait for response
      DatagramPacket responseDatagram;
      responseDatagram = clientConnection.sendPacketAndReceive(dataPacket);
            
      if (responseDatagram == null) {
        // TODO: handle timeouts
        System.err.println("Did not receive a response from the client.");
        return;
      }
      
      // 5. parse client response
      Acknowledgement ack = null;
      try {
       ack = packetParser.parseAcknowledgement(responseDatagram);
      } catch (InvalidAcknowledgementException e) {
        String errMsg = "Not a valid ACK: " + e.getMessage();
        handlePacketError(errMsg, responseDatagram);
        return;
      }
       
      // make sure the block number is correct
      if (ack.getBlockNumber() != blockNumber) {
        String errMsg = "ACK has the wrong block number, expected block #" + blockNumber;
        System.err.println(errMsg);
        handlePacketError(errMsg, responseDatagram);
        return;
      }
      
      System.out.println("Received ACK packet with block #" + ack.getBlockNumber());
      printPacketInformation(ack);
      
      // 6. repeat
      blockNumber++;
    } while (bytesRead == 512);
    
    fileReader.close();
    
    System.out.println("File " + request.getFilename() +" successfully sent to client in " + 
        (blockNumber - 1) + " blocks.");
  }
  
  private void receiveFileFromClient(WriteRequest request) {
    System.out.println("[SYSTEM] opening " + request.getFilename() + " for writing.");
    FileWriter fileWriter = null;
  
    try {
      fileWriter = new FileWriter(request.getFilename());
    } catch (FileAlreadyExistsException e) {
      // TODO Send error FILE_ALREADY_EXISTS
      e.printStackTrace();
      return;
    } catch (IOException e) {
      // TODO Send error DISK_FULL_OR_ALLOCATION_EXCEEDED 
      e.printStackTrace();
      return;
    }

    int blockNumber = 0;
    PacketParser packetParser = new PacketParser();
    
    while (true) {
      //1. build an ACK
      System.out.println("[SYSTEM] Sending ACK with block#" + blockNumber);
      
      Acknowledgement ack = new AcknowledgementBuilder()
          .setRemoteHost(request.getRemoteHost())
          .setRemotePort(request.getRemotePort())
          .setBlockNumber(blockNumber)
          .buildAcknowledgement();
  
      printPacketInformation(ack);
      
      //2. send ACK and wait for a data packet
      DatagramPacket receivedDatagram = clientConnection.sendPacketAndReceive(ack);
      
      if (receivedDatagram == null) {
        // TODO: handle timeouts
        System.err.println("Did not receive a data packet from the client.");
        return;
      }
      
      // we are now expecting the next sequential block number
      blockNumber++;
            
      DataPacket dataPacket;
      try {
        dataPacket = packetParser.parseDataPacket(receivedDatagram);
      } catch (InvalidDataPacketException e) {
        String errMsg = "Not a valid Data Packet: " + e.getMessage();
        System.err.println(errMsg);
        handlePacketError(errMsg, receivedDatagram);
        return;
      }
      
      // make sure the block number is correct
      if (dataPacket.getBlockNumber() != blockNumber) {
        String errMsg = "Data packet has the wrong block#, expected block #" + blockNumber;
        System.err.println(errMsg);
        handlePacketError(errMsg, receivedDatagram);
        return;
      }
      
      System.out.println("Received Data packet with block #" + dataPacket.getBlockNumber());
      printPacketInformation(dataPacket);
      
      //3. write file data to disk
      try {
        fileWriter.writeBlock(dataPacket.getFileData());
      } catch (IOException e) {
        // TODO send an DISK_FULL_OR_ALLOCATION_EXCEEDED error
        e.printStackTrace();
        return;
      }
      
      // was this the last data packet?
      if (dataPacket.getFileData().length < 512) {
        // send last ACK
        Acknowledgement lastAck = new AcknowledgementBuilder()
            .setRemoteHost(request.getRemoteHost())
            .setRemotePort(request.getRemotePort())
            .setBlockNumber(blockNumber)
            .buildAcknowledgement();
    
        System.out.println("[SYSTEM] Sending ACK with block#" + blockNumber);
        printPacketInformation(ack);
        clientConnection.sendPacket(lastAck);
        
        // we're done
        break;
      }
    }
    
    fileWriter.close();
    
    System.out.println("File " + request.getFilename() + " successfully received from client in " 
          + blockNumber + " blocks.");
  }
  
  private void handlePacketError(String message, DatagramPacket requestPacket) {
    ErrorPacket errPacket = new ErrorPacketBuilder()
        .setRemoteHost(requestPacket.getAddress())
        .setRemotePort(requestPacket.getPort())
        .setErrorCode(ErrorCode.ILLEGAL_TFTP_OPERATION)
        .setMessage(message)
        .buildErrorPacket();
    clientConnection.sendPacket(errPacket);
  }
    
  /**
   * Prints out request contents as a String and in bytes.
   * 
   * @param buffer
   */
  private void printPacketInformation(Packet packet) {
    byte[] data = packet.getPacketData();
    String contents = new String(data);

    System.out.println("\tPacket contents: ");
    System.out.println("\t" + contents);

    System.out.println("\tPacket contents (bytes): ");
    System.out.print("\t");
    for (int i = 0; i < data.length; i++) {
      System.out.print(data[i] + " ");
    }
    System.out.println();
  }
}