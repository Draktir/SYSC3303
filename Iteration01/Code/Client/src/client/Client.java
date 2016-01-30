/**
 * The Client class implements an application that will
 * send and receive TFTP requests to and from a server
 * 
 * @author  Loktin Wong
 * @version 1.0.0
 * @since 22-01-2016
 */

// TODO: add proper documentation to all new functions, and make changes to existing documentation.

package client;

import packet.*;
import client.FileReader;
import client.FileWriter;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
  public static final int SERVER_PORT = 6900;
  private ServerConnection serverConnection;
  private FileReader fileReader;
  private FileWriter fileWriter;
  private boolean transferComplete = false;

  /**
   * Default Client constructor which instantiates a DatagramSocket on an open
   * port on the local machine
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
   * Main method which creates an instance of Client to sends alternating read
   * and write TFTP requests to a server on the local machine at the SERVER_PORT
   * port.
   * 
   * @param args
   */
  public static void main(String[] args) {
    Client c = new Client();
    c.downloadFileFromServer("testReadFile.txt", "ocTeT");
    c.sendFileToServer("testWriteFile.txt", "netAsCiI");
    
  }

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
        .setRemotePort(SERVER_PORT)
        .setFilename(filename)
        .setMode(mode)
        .buildReadRequest();
    
    performFileTransfer(req);
  }

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
      
      System.out.println("\nPacket received from Server");
      
      if (recvdPacket != null) {
        printPacketInformation(recvdPacket);
      } else {
        System.out.println("File successfully transferred");
      }
      
      
    } while (!transferComplete);

    System.out.println("File transfer ended.");
  }

  private Packet handleAcknowledgement(Acknowledgement ack) {
    System.out.println("ACK received, block# " + ack.getBlockNumber());
    
    byte[] buffer = new byte[512];
    int bytesRead = fileReader.readBlock(buffer);

    byte[] fileData = new byte[bytesRead];
    System.arraycopy(buffer, 0, fileData, 0, bytesRead);

    System.out.println("FILE DATA LENGTH: " + bytesRead);
    
    DataPacket dataPacket = new DataPacketBuilder()
        .setRemoteHost(ack.getRemoteHost())
        .setRemotePort(ack.getRemotePort())
        .setBlockNumber(ack.getBlockNumber() + 1)
        .setFileData(fileData)
        .buildDataPacket();

    printPacketInformation(dataPacket);

    // Check if we have read the whole file
    if (fileData.length < 512) {
      System.out.println("Sending last data packet");
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

  private Packet handleDataPacket(DataPacket dataPacket) {
    System.out.println("Data Packet received, block# " + dataPacket .getBlockNumber());
    
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
      System.out.println("Acknowledging last data packet");
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
    String contents = new String(data);

    System.out.println("Packet contents: ");
    System.out.println(contents);

    System.out.println("Packet contents (bytes): ");
    for (int i = 0; i < data.length; i++) {
      System.out.print(data[i] + " ");
    }
    System.out.println();
  }
}
