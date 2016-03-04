package intermediate_host;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import Configuration.Configuration;
import packet.Packet;

public class intermediateHostConnection {
	
	  private DatagramSocket intermediateHostSocket;
	  private InetAddress intermediateHostAddress = null;
	  private int intermediateHostPort = -1;
	
	  public intermediateHostConnection() throws SocketException {
	    this.intermediateHostSocket = new DatagramSocket();
	    //this.serverSocket.setSoTimeout(Configuration.TIMEOUT_TIME);
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
	      intermediateHost.printPacketInformation(sendPacket);
	      
	      intermediateHost.send(sendPacket);
	    } catch (UnknownHostException e) {
	      e.printStackTrace();
	      System.exit(1);
	    } catch (IOException e) {
	      e.printStackTrace();
	      System.exit(1);
	    }
	  }
}
