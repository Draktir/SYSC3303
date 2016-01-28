package server;

import java.io.FileNotFoundException;
import java.net.SocketException;

import packet.Acknowledgement;
import packet.AcknowledgementBuilder;
import packet.DataPacket;
import packet.DataPacketBuilder;
import packet.InvalidPacketException;
import packet.Packet;
import packet.PacketParser;
import packet.ReadRequest;
import packet.WriteRequest;

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
  private Packet requestPacket;
  private boolean transferComplete = false;

  private ClientConnection clientConnection;
  private FileReader fileReader;
  private FileWriter fileWriter;

  
  /**
   * Default RequestHandler constructor instantiates requestPacekt to
   * the packet passed down from the Listener class.
   * 
   * @param packet
   */
  public RequestHandler(Packet requestPacket) {
    this.requestPacket = requestPacket;
  }
  
  public void run() {
    try {
      processRequest(requestPacket);
    } catch (InvalidPacketException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Processes a received DatagramPacket by testing it's contents
   * and responds appropriately.
   *  
   * @param packet
   * @throws InvalidPacketException
   */
  public void processRequest(Packet requestPacket) throws InvalidPacketException {
    try {
      this.clientConnection = new ClientConnection();
    } catch (SocketException e) {
      e.printStackTrace();
      System.err.println("Could not create client socket.");
      return;
    }
    
    PacketParser parser = new PacketParser();
    Packet request = parser.parse(requestPacket);
    
    do {
      printPacketInformation(request);
      
      if (request instanceof ReadRequest) {
        request = handleReadRequest((ReadRequest) request);
      } else if (request instanceof WriteRequest) {
        request = handleWriteRequest((WriteRequest) request);
      } else if (request instanceof DataPacket) {
        request = handleDataPacket((DataPacket) request);
      } else if (request instanceof Acknowledgement) {
        request = handleAcknowledgement((Acknowledgement) request);
      } else {
        System.err.println("Invalid request received, closing connection.");
        break;
      }
    } while (!transferComplete);
  }
  
  /**
   * Handles a received Read request (RRQ) by reading the requested
   * block from disk and responding with a Data packet
   * 
   * @param request
   */
  private Packet handleReadRequest(ReadRequest request) {
    try {
      fileReader = new FileReader(request.getFilename());
    } catch (FileNotFoundException e) {
      // TODO: send an error packet if the file does not exist
      e.printStackTrace();
    }
    return sendFileBlock(request, 1);
  }
  
  /**
   * Handles a received Write request (WRQ) by writing the received data
   * to disk and responding with an ACK
   * 
   * @param request
   */
  private Packet handleWriteRequest(WriteRequest request) {
    try {
      fileWriter = new FileWriter(request.getFilename());
    } catch (FileNotFoundException e) {
      // TODO: send an error packet if something goes wrong here
      e.printStackTrace();
    }
    
    System.out.println("Sending ACK with block# 0");
    
    Acknowledgement ack = new AcknowledgementBuilder()
            .setRemoteHost(request.getRemoteHost())
            .setRemotePort(request.getRemotePort())
            .setBlockNumber(0)
            .buildAcknowledgement();
    
    printPacketInformation(ack);
    return clientConnection.sendPacketAndReceive(ack);
  }
  
  /**
   * Handles a received Data packet by writing the received data
   * to disk and responding with an ACK
   * 
   * @param packet
   */
  private Packet handleDataPacket(DataPacket dataPacket) {
    System.out.println("Writing file block# " + dataPacket.getBlockNumber());
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
      transferComplete = true;
      fileWriter.close();
      // send the last ACK
      clientConnection.sendPacket(ack);
      return null;
    }
    
    return clientConnection.sendPacketAndReceive(ack);
  }
  
  /**
   * Handles a received ACK by sending the next file block.
   * 
   * @param packet
   */
  private Packet handleAcknowledgement(Acknowledgement ackPacket) {
    return sendFileBlock(ackPacket, ackPacket.getBlockNumber());
  }
  
  private Packet sendFileBlock(Packet request, int blockNumber) {
    System.out.println("Reading block# " + blockNumber +" from file.");
    
    byte[] buffer = new byte[512];
    int bytesRead = fileReader.readBlock(buffer);
    
    byte[] fileData = new byte[bytesRead];
    System.arraycopy(buffer, 0, fileData, 0, bytesRead);
    
    DataPacket dataPacket = new DataPacketBuilder()
            .setRemoteHost(request.getRemoteHost())
            .setRemotePort(request.getRemotePort())
            .setBlockNumber(blockNumber)
            .setFileData(fileData) 
            .buildDataPacket();
    
    printPacketInformation(dataPacket);
    
    // Check if we have read the whole file
    if (fileData.length < 512) {
      transferComplete = true;
      fileReader.close();
      
      // send the last data packet
      clientConnection.sendPacketAndReceive(dataPacket);

      // TODO: We should make sure we get an ACK and resend the last data packet
      // if it failed. Not needed for this iteration though.
      return null;
    }
    
    return clientConnection.sendPacketAndReceive(dataPacket);
  }
  
  /**
   * Prints out packet contents as a String and in bytes.
   * 
   * @param packet
   */
  public void printPacketInformation(Packet packet) {
    byte[] data = packet.getPacketData();
    String contents = new String(data);
    
    System.out.println("Request contents: ");
    System.out.println(contents);
    
    System.out.println("Request contents (bytes): ");
    for (int i = 0; i < data.length; i++) {
      System.out.print(data[i] + " ");
    }
    System.out.println();
  }
}