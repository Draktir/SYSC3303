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

import java.util.Date;

import Configuration.Configuration;

import java.sql.Timestamp;


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
    boolean errorOccured = false;
    log("opening " + request.getFilename() + " for reading.");
    
    FileReader fileReader = null;
    try {
      fileReader = new FileReader(request.getFilename());
    } catch (FileNotFoundException e) {
      log("opening " + request.getFilename() + " for reading failed. File not found!");
      // TODO Send an Error (FILE_NOT_FOUND)
      e.printStackTrace();
      errorOccured = true;
    }
    
    // check file size before sending
    File f = new File(request.getFilename());
    final long MAX_FILE_SIZE = (long) ((Math.pow(2, 16) - 1) * 512);
    if (f.length() > MAX_FILE_SIZE) {
      log("The requested file is too big: " + f.length() + " bytes. Max is " + MAX_FILE_SIZE + " bytes.");
      errorOccured = true;
    }

    if (errorOccured) {
      log("Internal Server Error. Stopping.");
      if (fileReader != null) {
        fileReader.close();
      }
      return;
    }
    
    int blockNumber = 0;
    int bytesRead;
    byte[] fileBuffer = new byte[512];
    
    do {
      // reset socket timeout
      clientConnection.setTimeOut(Configuration.TIMEOUT_TIME);
      
      blockNumber++;
      // 1. read the next block from the file 
      log("reading block #" + blockNumber + " from file.");
      bytesRead = fileReader.readNextBlock(fileBuffer);
      bytesRead = bytesRead >= 0 ? bytesRead : 0;
      
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
            
      int sendAttempts = 0;
      Acknowledgement ack = null;
      
      do {
        log("sending data packet, block #" + dataPacket.getBlockNumber());
        log(dataPacket.toString());
        
        // 4. send data packet and wait for response
        clientConnection.sendPacket(dataPacket);
        
        while (ack == null) {
          // 5. wait for a response
          log("expecting ACK, block #" + blockNumber);
          long tsStart = currentTime();
          DatagramPacket responseDatagram = clientConnection.receive();
          long tsStop = currentTime();
          
          // will receive null if socket timed out
          if (responseDatagram == null) {
            log("Did not receive a response from the client.");
            break;
          }
          
          log("Received packet.");
          printPacketInformation(responseDatagram);
          
          // 6. parse client response
          try {
            ack = packetParser.parseAcknowledgement(responseDatagram);
          } catch (InvalidAcknowledgementException e) {
            String errMsg = "Not a valid ACK: " + e.getMessage();
            log(errMsg);
            handleParseError(errMsg, responseDatagram);
            errorOccured = true;
            break;
          }
  
          // check for duplicate. If the ack is duplicate, just ignore.
          if (ack.getBlockNumber() < blockNumber) {
            log("Received duplicate ACK with block #" + ack.getBlockNumber());
            clientConnection.setTimeOut(tsStop - tsStart);
            ack = null;
            continue;
          } else if (ack.getBlockNumber() > blockNumber) { 
            // if it's not a duplicate, send an error and terminate
            String errMsg = "ACK has the wrong block#, got #" + ack.getBlockNumber() + "expected #" + blockNumber;
            log(errMsg);
            sendErrorPacket(errMsg, responseDatagram);
            errorOccured = true;
            break;
          }
        }

        // reset socket timeout for retries
        clientConnection.setTimeOut(Configuration.TIMEOUT_TIME);
      } while(sendAttempts++ < Configuration.MAX_RETRIES && ack == null && !errorOccured);
      
      // did we exceed max retries?
      if (sendAttempts >= Configuration.MAX_RETRIES) {
        log("Retried sending DataPacket " + Configuration.MAX_RETRIES + " times. Giving up.");
        errorOccured = true;
        break;
      }
      
      log("Received valid ACK packet\n" + ack.toString() + "\n");
    } while (bytesRead == 512 && !errorOccured);

    if (fileReader != null) {
      fileReader.close();
    }
    
    if (errorOccured) {
      log("Error occured. No file was transferred");
    } else {
      log("File " + request.getFilename() +" successfully sent to client in " + blockNumber + " blocks.");
    }
  }
  
  private void receiveFileFromClient(WriteRequest request) {
    log("Opening " + request.getFilename() + " for writing.");
    FileWriter fileWriter = null;
    boolean errorOccured = false;
  
    try {
      fileWriter = new FileWriter(request.getFilename());
    } catch (FileAlreadyExistsException e) {
      // TODO this exception is not thrown in this iteration. The file is simply overwritten
      // TODO Send error FILE_ALREADY_EXISTS
      log("opening " + request.getFilename() + " for writing failed. File already exists!");
      e.printStackTrace();
      errorOccured = true;
    } catch (IOException e) {
      // TODO Send error DISK_FULL_OR_ALLOCATION_EXCEEDED
      log("opening " + request.getFilename() + " for writing failed." + " Disk is full!");
      e.printStackTrace();
      errorOccured = true;
    }

    int blockNumber = 0;
    
    while (!errorOccured) {
      //1. build an ACK
      Acknowledgement ack = new AcknowledgementBuilder()
          .setRemoteHost(request.getRemoteHost())
          .setRemotePort(request.getRemotePort())
          .setBlockNumber(blockNumber)
          .buildAcknowledgement();
      
      int sendAttempts = 0;
      DataPacket dataPacket = null;
      
      // now expecting data packet with next block number
      blockNumber++;
      
      do {
        log("Sending ACK");
        log(ack.toString());
        // 2. send ack
        clientConnection.sendPacket(ack);
        
        while (dataPacket == null) {
          // 3. wait for a response
          log("expecting Data, block #" + blockNumber);
          long tsStart = currentTime();
          DatagramPacket responseDatagram = clientConnection.receive();
          long tsStop = currentTime();
          
          // will receive null if socket timed out
          if (responseDatagram == null) {
            log("Did not receive a response from the client.");
            break;
          }
          
          log("Received packet.");
          printPacketInformation(responseDatagram);
          
          // 4. parse client response
          try {
            dataPacket = packetParser.parseDataPacket(responseDatagram);
          } catch (InvalidDataPacketException e) {
            String errMsg = "Not a valid DataPacket: " + e.getMessage();
            log(errMsg);
            handleParseError(errMsg, responseDatagram);
            errorOccured = true;
            break;
          }
  
          // check for duplicate. If the data is duplicate, send ACK and ignore data.
          if (dataPacket.getBlockNumber() < blockNumber) {
            log("Received duplicate DataPacket with block #" + dataPacket.getBlockNumber());
            
            Acknowledgement ackForWrongData = new Acknowledgement(dataPacket.getRemoteHost(), dataPacket.getRemotePort(), 
                dataPacket.getBlockNumber());
            clientConnection.sendPacket(ackForWrongData);
            clientConnection.setTimeOut(tsStop - tsStart);
            dataPacket = null;
            continue;
          } else if (dataPacket.getBlockNumber() > blockNumber) { 
            // if it's not a duplicate, send an error and terminate
            String errMsg = "DataPacket has the wrong block#, got #" + dataPacket.getBlockNumber() + "expected #" + blockNumber;
            log(errMsg);
            sendErrorPacket(errMsg, responseDatagram);
            errorOccured = true;
            break;
          }
        }
        
        // reset socket timeout for retries
        clientConnection.setTimeOut(Configuration.TIMEOUT_TIME);
      } while(sendAttempts++ < Configuration.MAX_RETRIES && dataPacket == null && !errorOccured);
      
      // did we exceed max retries?
      if (sendAttempts >= Configuration.MAX_RETRIES) {
        log("Retried sending DataPacket " + Configuration.MAX_RETRIES + " times. Giving up.");
        errorOccured = true;
        break;
      }
      
      // was there an error?
      if (errorOccured) {
        break;
      }
      
      log("Received valid DataPacket\n" + dataPacket.toString() + "\n");
      log("Writing data to disk");
      
      //3. write file data to disk
      try {
        fileWriter.writeBlock(dataPacket.getFileData());
      } catch (IOException e) {
        System.err.println("Disk is full or allocation exceeded");  
        // TODO send an DISK_FULL_OR_ALLOCATION_EXCEEDED error
        e.printStackTrace();
        errorOccured = true;
        break;
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
        fileWriter.close();
        break;
      }
      
      log("");
    }
    
    fileWriter.close();
    
    if (errorOccured) {
      System.out.println("Error occured. Deleting file.");
      new File(request.getFilename()).delete();
    } else {
      log("File " + request.getFilename() + " successfully received from client in " 
          + blockNumber + " blocks.");
    }
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
   
  public static long currentTime() {
    Date d = new Date();
    return new Timestamp(d.getTime()).getTime();
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