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
import packet.WriteRequest;
import packet.ErrorPacket.ErrorCode;

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
    Packet request = null;
    
    // 1. parse the orignal client request
    try {
      parser.parseRequest(requestPacket);
    } catch (InvalidRequestException e) {
      System.err.println("Received an invalid request.");
      handlePacketError("Invalid request. Expected a RRQ or WRQ.", requestPacket);
      System.out.println("Terminating this thread.");
      return;
    }
    
    // 2. determine the type of transfer
    if (request instanceof ReadRequest) {
      // initiate ReadRequest
      sendFileToClient((ReadRequest) request);
    } else if (request instanceof WriteRequest) {
      // initiate WriteRequest
      receiveFileFromClient((WriteRequest) request);
    } else {
      // should never get here really
      System.err.println("Could not identify request type. SOMETHING IS SERIOUSLY WRONG!");
      handlePacketError("Invalid request. Expected RRQ or WRQ.", requestPacket);
    }
    
    System.out.println("Terminating thread " + Thread.currentThread().getName());
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
        System.err.println("Did not receive a response from the client.");
        break;
      }
      
      // 5. parse client response
      Acknowledgement ack = null;
      try {
       ack = packetParser.parseAcknowledgement(responseDatagram);
      } catch (InvalidAcknowledgementException e) {
        handlePacketError("Did not receive a valid ACK. Expected ACK with block #" + blockNumber, 
            responseDatagram);
        return;
      }
       
      // make sure the block number is correct
      if (ack.getBlockNumber() != blockNumber) {
        handlePacketError("Acknowledgement had the wrong block#, expected block #" + blockNumber, 
            responseDatagram);
        return;
      }
      
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
      //1. send an ACK
      System.out.println("[SYSTEM] Sending ACK with block#" + blockNumber);
      
      Acknowledgement ack = new AcknowledgementBuilder()
          .setRemoteHost(request.getRemoteHost())
          .setRemotePort(request.getRemotePort())
          .setBlockNumber(blockNumber)
          .buildAcknowledgement();
  
      printPacketInformation(ack);
      DatagramPacket receivedDatagram = clientConnection.sendPacketAndReceive(ack);
      
      // we are now expecting the next sequential block number
      blockNumber++;
      
      //2. wait for a data packet
      DataPacket dataPacket;
      try {
        dataPacket = packetParser.parseDataPacket(receivedDatagram);
      } catch (InvalidDataPacketException e) {
        handlePacketError("Did not receive a valid Data Packet. Expected Data packet with block #" 
            + blockNumber, receivedDatagram);
        return;
      }
      
      // make sure the block number is correct
      if (dataPacket.getBlockNumber() != blockNumber) {
        handlePacketError("Data packet had the wrong block#, expected block #" + blockNumber, 
            receivedDatagram);
        return;
      }
      
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
        System.out.println("[SYSTEM] Sending ACK with block#" + blockNumber);
        
        Acknowledgement lastAck = new AcknowledgementBuilder()
            .setRemoteHost(request.getRemoteHost())
            .setRemotePort(request.getRemotePort())
            .setBlockNumber(blockNumber)
            .buildAcknowledgement();
    
        printPacketInformation(ack);
        clientConnection.sendPacket(lastAck);
        
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