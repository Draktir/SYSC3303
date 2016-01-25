import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.UnknownHostException;

import packet.AcknowledgementBuilder;
import packet.DataPacketBuilder;
import packet.Packet;
import packet.PacketBuilder;
import request.ReadRequest;
import request.Request;
import request.RequestParser;
import request.WriteRequest;

/**
 * The RequestHandler class handles requests received by Listener.
 * It creates a separate thread (needs testing) for each Client 
 * request that is received.
 * 
 * @author Loktin Wong
 * @version 1.0.1
 * @since 25-01-2016
 */
class RequestHandler implements Runnable {
  DatagramPacket receivePacket;
  int sendFileBlockNumber = 0;
  int receiveFileBlockNumber = 0;
  
  /**
   * Default RequestHandler constructor instantiates receivePacket to
   * the packet passed down from the Listener class.
   * 
   * @param packet
   */
  public RequestHandler(DatagramPacket packet) {
    this.receivePacket = packet;
  }
  
  public void run() {
    try {
      processRequest(receivePacket);
    } catch (Exception e) {
      // Error message is printed inside processRequest(DatagramPacket)
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
  public void processRequest(DatagramPacket packet) throws Exception {
    // copy data out of the buffer and into an array
    int len = receivePacket.getLength();
    byte[] data = new byte[len]; 
    System.arraycopy(packet.getData(), 0, data, 0, len);
    
    PacketBuilder packetBuilder = new PacketBuilder();
    packetBuilder.setRemoteHost(packet.getAddress());
    packetBuilder.setRemotePort(packet.getPort());
    packetBuilder.setPacketData(data);
    
    RequestParser reqParser = new RequestParser();
    Request request = reqParser.parse(packetBuilder.buildGenericPacket());
    
    if (request instanceof ReadRequest) {
      handleReadRequest((ReadRequest) request);
    } else if (request instanceof WriteRequest) {
      handleWriteRequest((WriteRequest) request);
    } else {
      System.err.println("Invalid request received");
    }
  }
  
  private void handleReadRequest(ReadRequest request) {
    //TODO read file from disk
    System.out.println("Need to read file from disk");
    byte[] fileData = {1, 24, 1, 22, 100};
    
    DataPacketBuilder builder = new DataPacketBuilder();
    builder.setRemoteHost(request.getRemoteHost());
    builder.setRemotePort(request.getRemotePort());
    builder.setBlockNumber(sendFileBlockNumber++);
    builder.setFileData(fileData);
    sendPacket(builder.buildDataPacket());
  }
  
  private void handleWriteRequest(WriteRequest request) {
    //TODO write file to disk
    System.out.println("Need to write file to disk");
    System.out.println("Sending ACK");
    
    AcknowledgementBuilder builder = new AcknowledgementBuilder();
    builder.setRemoteHost(request.getRemoteHost());
    builder.setRemotePort(request.getRemotePort());
    builder.setBlockNumber(receiveFileBlockNumber);
    
    sendPacket(builder.buildAcknowledgement());
    
    receiveFileBlockNumber++;
  }
    
  private void sendPacket(Packet packet) {
    try {
      byte[] data = packet.getPacketData();
      DatagramPacket sendPacket = new DatagramPacket(
          data, data.length, packet.getRemoteHost(), packet.getRemotePort());
      DatagramSocket tempSock = new DatagramSocket();
      System.out.println("[SYSTEM] Sending response to client at port " + packet.getRemotePort());
      printRequestInformation(data);
      tempSock.send(sendPacket);
      tempSock.close();
    }
    catch (UnknownHostException e) {
      e.printStackTrace();
      System.exit(1);
    }
    catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  /**
   * Prints out request contents as a String and in bytes.
   * 
   * @param buffer
   */
  public void printRequestInformation(byte[] buffer) {
    String contents = new String(buffer);
    
    System.out.println("Request contents: ");
    System.out.println(contents);
    
    System.out.println("Request contents (bytes): ");
    for (int i = 0; i < buffer.length; i++) {
      System.out.print(buffer[i] + " ");
    }
    System.out.println();
  }
}