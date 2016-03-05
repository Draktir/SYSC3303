package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
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
   * Sends a packet, but does not wait for a response.
   * 
   * @param packet
   */
  public void sendPacket(Packet packet) {
    try {
      byte[] data = packet.getPacketData();
      DatagramPacket sendPacket = new DatagramPacket(data, data.length, packet.getRemoteHost(), packet.getRemotePort());
      
      System.out.println("[SERVER-CONNECTION] Sending packet to server on port " + packet.getRemotePort());
      Client.printPacketInformation(sendPacket);
      
      serverSocket.send(sendPacket);
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
  
  /*
   * Just waiting for a packet. 
   */

  public DatagramPacket recieve(){
	  	byte[] buffer;
	    DatagramPacket responseDatagram = null;
	    
	    do {
	      buffer = new byte[517];
	      responseDatagram = new DatagramPacket(buffer, 517);
	      
	      long tsStart = currentTime();
	      try {
	        System.out.println("[SERVER-CONNECTION] Waiting for response from server on port " + serverSocket.getLocalPort());
	        serverSocket.receive(responseDatagram);
	      } catch (SocketTimeoutException e) {
	        System.out.println("[SERVER-CONNECTION] Response timed out.");
	        return null;  
	      } catch (IOException e) {
	        e.printStackTrace();
	        return null;
	      }
	      long tsStop = currentTime();
	      
	      // ensure the client TID is the same
	      if (!isServerTidValid(responseDatagram)) {
	        System.err.println("[SERVER-CONNECTION] Received packet with wrong TID");
	        System.err.println("  > Client:   " + responseDatagram.getAddress() + " " + responseDatagram.getPort());
	        System.err.println("  > Expected: " + serverSocket + " " + serverSocket);
	        // respond to the rogue client with an appropriate error packet
	        ErrorPacket errPacket = new ErrorPacketBuilder()
	            .setErrorCode(ErrorCode.UNKNOWN_TRANSFER_ID)
	            .setMessage("Your request had an invalid TID.")
	            .setRemoteHost(responseDatagram.getAddress())
	            .setRemotePort(responseDatagram.getPort())
	            .buildErrorPacket();
	        
	        System.err.println("[SERVER-CONNECTION] Sending error to server with invalid TID\n" + errPacket.toString() + "\n");
	        
	        byte[] errData = errPacket.getPacketData();
	        DatagramPacket errDatagram = new DatagramPacket(errData, errData.length,
	            errPacket.getRemoteHost(), errPacket.getRemotePort());
	        
	        try {
	        	serverSocket.send(errDatagram);
	        } catch (IOException e) {
	          e.printStackTrace();
	          System.err.println("[SERVER-CONNECTION] Error sending error packet to unknown server. Ignoring this error.");
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
	    
	    System.err.println("[SERVER-CONNECTION] Waiting for another packet.");
	    return responseDatagram;
  }
  /**
   * Sends a packet and blocks until a response is received on the same socket
   * 
   * @param packet
   * @return GenericPacket containing data and connection information
   */
  public DatagramPacket sendPacketAndReceive(Packet packet) {
    byte[] data = packet.getPacketData();
    DatagramPacket sendPacket = new DatagramPacket(data, data.length, packet.getRemoteHost(), packet.getRemotePort());
    
    System.out.println("[SERVER-CONNECTION] Sending packet to server on port " + packet.getRemotePort());
    Client.printPacketInformation(sendPacket);
    
    try {
      serverSocket.send(sendPacket);
    } catch (IOException e1) {
      e1.printStackTrace();
      return null;
    }

    byte[] buffer = null;
    DatagramPacket responseDatagram = null;
    
    do {
      buffer = new byte[517];
      responseDatagram = new DatagramPacket(buffer, 517);
      boolean packetReceived = false;
      
      while (!packetReceived) {
    	System.out.println("[SERVER-CONNECTION] Waiting for response from server on port " + serverSocket.getLocalPort());
	    try {
	      serverSocket.receive(responseDatagram);
	      packetReceived = true;
	    } catch (SocketTimeoutException e) {
	      System.out.println("[SERVER-CONNECTION] Response timed out. Attempting to resend last packet.");
	      try {
	        serverSocket.send(sendPacket);
	      } catch (IOException e1) {
	        e1.printStackTrace();
	        return null;
	      }
	    } catch (IOException e) {
	      e.printStackTrace();
	      return null;
	    } 
      }
      
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
        
        System.err.println("[SERVER-CONNECTION] Sending error to server with invalid TID\n" + errPacket.toString() + "\n");
        
        byte[] errData = errPacket.getPacketData();
        DatagramPacket errDatagram = new DatagramPacket(errData, errData.length,
            errPacket.getRemoteHost(), errPacket.getRemotePort());
        
        try {
          serverSocket.send(errDatagram);
        } catch (IOException e) {
          e.printStackTrace();
          System.err.println("[SERVER-CONNECTION] Error sending error packet to unknown server. Ignoring this error.");
        }
        responseDatagram = null;
        
        System.err.println("[SERVER-CONNECTION] Waiting for another packet.");
      }
    } while (responseDatagram == null);
    
    Client.printPacketInformation(responseDatagram);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public static long currentTime() {
	    Date d = new Date();
	    return new Timestamp(d.getTime()).getTime();
	  }
}
