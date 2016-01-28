package server;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;

import java.io.IOException;

import packet.Acknowledgement;
import packet.AcknowledgementBuilder;
import packet.DataPacket;
import packet.DataPacketBuilder;
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
  private ClientConnection clientConnection;
  private String filename;
  private int blockNumber = 0;
  private boolean transferComplete = false;
  
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
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Processes a received DatagramPacket by testing it's contents
   * and responds appropriately.
   *  
   * @param packet
   * @throws Exception not yet implemented
   */
  public void processRequest(Packet requestPacket) throws Exception {
    this.clientConnection = new ClientConnection();
    
    PacketParser parser = new PacketParser();
    Packet request = parser.parse(requestPacket);
    
    do {
      if (request instanceof ReadRequest) {
        request = handleReadRequest((ReadRequest) request);
      } else if (request instanceof WriteRequest) {
        request = handleWriteRequest((WriteRequest) request);
      } else if (request instanceof DataPacket) {
        request = handleDataPacket((DataPacket) request);
      } else if (request instanceof Acknowledgement) {
        request = handleAcknowledgement((Acknowledgement) request);
      } else {
        System.err.println("Invalid request received");
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
    filename = request.getFilename();
    return sendFileBlock(request);
  }
  
  /**
   * Handles a received Write request (WRQ) by writing the received data
   * to disk and responding with an ACK
   * 
   * @param request
   */
  private Packet handleWriteRequest(WriteRequest request) {
    filename = request.getFilename();
    System.out.println("Sending ACK with block# 0");
    
    AcknowledgementBuilder builder = new AcknowledgementBuilder();
    builder.setRemoteHost(request.getRemoteHost());
    builder.setRemotePort(request.getRemotePort());
    builder.setBlockNumber(0);
    
    return clientConnection.sendPacketAndReceive(builder.buildAcknowledgement());
  }
  
  /**
   * Handles a received Data packet by writing the received data
   * to disk and responding with an ACK
   * 
   * @param packet
   */
  private Packet handleDataPacket(DataPacket packet) {
    byte[] fileData = packet.getFileData();
    System.out.println("Need to write to file block# " + blockNumber);
    
    File f = null;
    FileOutputStream fStream = null;
    
    // TODO: test if this works
    
    try {
		f = new File(filename);
		fStream = new FileOutputStream(f);
		fStream.write(fileData);
		fStream.flush();
	} catch (FileNotFoundException e) {
		// TODO: Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO: handle this error from bStream.write();
		e.printStackTrace();
	}
    
    AcknowledgementBuilder builder = new AcknowledgementBuilder();
    builder.setRemoteHost(packet.getRemoteHost());
    builder.setRemotePort(packet.getRemotePort());
    builder.setBlockNumber(packet.getBlockNumber());
    
    blockNumber++;
    
    // Check for the last data packet
    if (fileData.length < 512) {
      transferComplete = true;
      try {
		fStream.close();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
      clientConnection.sendPacket(builder.buildAcknowledgement());
      return null;
    } else {
      return clientConnection.sendPacketAndReceive(builder.buildAcknowledgement());
    }
  }
  
  /**
   * Handles a received ACK by sending the next file block.
   * 
   * @param packet
   */
  private Packet handleAcknowledgement(Acknowledgement packet) {
    return sendFileBlock(packet);
  }
  
  private Packet sendFileBlock(Packet request) {
    System.out.println("Need to read from file block# " + blockNumber);
    
    File f = null;
    FileInputStream fStream = null;
    byte[] fileData = null; 
    
    // TODO: test if this works
    
    try {
		f = new File(filename);
		fStream = new FileInputStream(f);
		int byteOffset = blockNumber * 512;
		long remainingBytes = f.length() - byteOffset;
		fileData = new byte[(int) Math.min(remainingBytes, 512)];
		fStream.read(fileData, byteOffset, (int) Math.min(remainingBytes, 512));
	} catch (FileNotFoundException e) {
		// TODO: Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO: handle this error from bStream.write();
		e.printStackTrace();
	}
    
    DataPacketBuilder builder = new DataPacketBuilder();
    builder.setRemoteHost(request.getRemoteHost());
    builder.setRemotePort(request.getRemotePort());
    builder.setBlockNumber(blockNumber);
    builder.setFileData(fileData);
    
    blockNumber++;
    
    DataPacket dataPacket = builder.buildDataPacket();
    
    // Check if we have the whole file
    if (fileData.length < 512) {
      transferComplete = true;
	  try {
		  fStream.close();
	  } catch (IOException e) {
			// TODO Auto-generated catch block
		e.printStackTrace();
	  }
      clientConnection.sendPacketAndReceive(dataPacket);
      // TODO: We should make sure we get an ACK and resend the last data packet
      // if it failed. Not needed for this assignment though.
      return null;
    }
    
    printPacketInformation(dataPacket);
    return clientConnection.sendPacketAndReceive(dataPacket);
  }
  
  /**
   * Prints out request contents as a String and in bytes.
   * 
   * @param buffer
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