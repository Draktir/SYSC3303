package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.Date;

import packet.ErrorPacket;
import packet.ErrorPacketBuilder;
import packet.Packet;
import packet.ErrorPacket.ErrorCode;
import Configuration.Configuration;

public class ServerConnection {
  private DatagramSocket serverSocket;
  private InetAddress serverAddress = null;
  private int serverPort = -1;

  public ServerConnection() throws SocketException {
    this.serverSocket = new DatagramSocket();
    this.serverSocket.setSoTimeout(Configuration.TIMEOUT_TIME);
  }

  /**
   * Sends a packet.
   * 
   * @param packet
   */
  public void sendPacket(Packet packet) {
    byte[] data = packet.getPacketData();
    DatagramPacket sendDatagram = new DatagramPacket(
        data, data.length, packet.getRemoteHost(), packet.getRemotePort());
    
    System.out.println("[CLIENT-CONNECTION] sending packet");
    Client.printPacketInformation(sendDatagram);
    
    try {
      serverSocket.send(sendDatagram);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  
  public DatagramPacket receive() {
    byte[] buffer;
    DatagramPacket responseDatagram = null;
    
    do {
      buffer = new byte[517];
      responseDatagram = new DatagramPacket(buffer, 517);
      
      long tsStart = Client.currentTime();
      try {
        System.out.println("[SERVER-CONNECTION] Waiting for response from client on port " + serverSocket.getLocalPort());
        serverSocket.receive(responseDatagram);
      } catch (SocketTimeoutException e) {
        System.out.println("[SERVER-CONNECTION] Response timed out.");
        return null;  
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
      long tsStop = Client.currentTime();
      
      // ensure the client TID is the same
      if (!isServerTidValid(responseDatagram)) {
        System.err.println("[SERVER-CONNECTION] Received packet with wrong TID");
        System.err.println("  > Server:   " + responseDatagram.getAddress() + " " + responseDatagram.getPort());
        System.err.println("  > Expected: " + serverAddress + " " + serverPort);
        // respond to the rogue client with an appropriate error packet
        ErrorPacket errPacket = new ErrorPacketBuilder()
            .setErrorCode(ErrorCode.UNKNOWN_TRANSFER_ID)
            .setMessage("Your request had an invalid TID.")
            .setRemoteHost(responseDatagram.getAddress())
            .setRemotePort(responseDatagram.getPort())
            .buildErrorPacket();
        
        System.err.println("[SERVER-CONNECTION] Sending error to client with invalid TID\n" + errPacket.toString() + "\n");
        
        byte[] errData = errPacket.getPacketData();
        DatagramPacket errDatagram = new DatagramPacket(errData, errData.length,
            errPacket.getRemoteHost(), errPacket.getRemotePort());
        
        try {
          serverSocket.send(errDatagram);
        } catch (IOException e) {
          e.printStackTrace();
          System.err.println("[SERVER-CONNECTION] Error sending error packet to unknown client. Ignoring this error.");
        }
        responseDatagram = null;
        
        System.err.println("[SERVER-CONNECTION] Waiting for another packet.");
        
        // reduce timeout
        try {
          serverSocket.setSoTimeout((int)(tsStop - tsStart));
        } catch (SocketException e) {
          e.printStackTrace();
        }
      }
    } while (responseDatagram == null);
    
    // reset timeout to original value
    try {
      serverSocket.setSoTimeout(Configuration.TIMEOUT_TIME);
    } catch (SocketException e) {
      e.printStackTrace();
    }
    
    System.out.println("[SERVER-CONNECTION] Received packet from client, length " + responseDatagram.getLength());
    return responseDatagram;
  }
  
  private boolean isServerTidValid(DatagramPacket packet) {
    if (serverAddress == null) return true;
    return serverAddress.equals(packet.getAddress()) && packet.getPort() == serverPort;
  }

  public void resetTid() {
    this.serverAddress = null;
    this.serverPort = -1;
  }
  
  public InetAddress getServerAddress() {
    return serverAddress;
  }

  public void setServerAddress(InetAddress serverAddress) {
    this.serverAddress = serverAddress;
  }

  public int getServerPort() {
    return serverPort;
  }

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setTimeOut(long milSec) {
		try {
			this.serverSocket.setSoTimeout((int) milSec);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	public static long currentTime() {
	    Date d = new Date();
	    return new Timestamp(d.getTime()).getTime();
	  }
}
