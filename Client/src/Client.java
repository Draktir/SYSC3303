<<<<<<< HEAD
import java.io.*;
import java.net.*;
import packet.*;

public class Client {

   private DatagramPacket rqstPacket, akg,dataPacket;
   private DatagramSocket sendReceiveSocket;
   private DataPacket d;
   private ReadRequest rrqst;
   private WriteRequest wrqst;
   

   public Client()
   {
      try {
         sendReceiveSocket = new DatagramSocket();
      } catch (SocketException se) {   // Can't create the socket.
         se.printStackTrace();
         System.exit(1);
      }
   }

   private void sendAndReceive()
   {
	   try {
		wrqst=new WriteRequest(InetAddress.getLocalHost(),68,InetAddress.getLocalHost(),00,"out.dat","ASCII");
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	   
	   try {
		rrqst=new ReadRequest(InetAddress.getLocalHost(),68,InetAddress.getLocalHost(),00,"in.dat","ASCII");
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	   
	   try {
		rqstPacket = new DatagramPacket(wrqst.getPacketData(), wrqst.getPacketData().length,InetAddress.getLocalHost(), 68);
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}//writeRequest
	   
	   try {
		sendReceiveSocket.send(rqstPacket);
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	   System.out.println("write request sent.");
	   byte data[] = new byte[4];
	   akg = new DatagramPacket(data, data.length);
	   try {
		sendReceiveSocket.receive(akg);
	} catch (IOException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	   System.out.println("Akg received: "+akg);
	   //check the akg
	   
	   try {
		readAndSend();
	} catch (UnknownHostException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	   System.out.println("data sending finished");
       sendReceiveSocket.close();
   }

   private void readAndSend() throws UnknownHostException, IOException{
	   BufferedInputStream in = new BufferedInputStream(new FileInputStream("in.dat"));
	   byte[] data = new byte[512];
	   int n;
	   while ((n = in.read(data)) != -1) {
		   d=new DataPacket(InetAddress.getLocalHost(),68,n,data);
		   dataPacket=new DatagramPacket(d.getPacketData(),d.getPacketData().length);
		   sendReceiveSocket.send(dataPacket);
	   }
	   in.close();
   }

   public static void main(String args[])
   {
      Client c = new Client();
      c.sendAndReceive();
   }
}
=======
/**
 * The Client class implements an application that will
 * send and receive TFTP requests to and from a server
 * 
 * @author  Loktin Wong
 * @version 1.0.0
 * @since 22-01-2016
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
	
	public static final int SERVER_PORT = 69;
	public static final int REQUESTS = 11;
	
	DatagramSocket srSocket;
	DatagramPacket sendPacket, receivePacket;
	
	/**
	 * Default Client constructor which instantiates a 
	 * DatagramSocket on an open port on the local machine
	 */
	public Client() {
		try {
			srSocket = new DatagramSocket();
		}
		catch (SocketException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	/**
	 * Main method which creates an instance of Client to
	 * sends alternating read and write TFTP requests to 
	 * a server on the local machine at the SERVER_PORT port.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Client c = new Client();
		
		for (int i = 0; i < REQUESTS; i++) {
			if (i % 2 == 0 && i < 10) 
				c.sendRequest(1, "launch_codes.txt", "NeTaScIi");
			else if (i % 2 == 1 && i < 10)
				c.sendRequest(2, "xkcd_ideas.jpg", "oCtEt");
			else 
				c.sendRequest(1, "", "packet");
		}
	}
	
	/**
	 * Sends a TFTP request to a TFTP server based on the type
	 * of request.
	 * 
	 * @param type used to determine what type of request should be sent
	 * @param file name of the file the client is requesting
	 * @param mode the mode in which a file is encoded
	 */
	public void sendRequest(int type, String file, String mode) {
		byte[] fileName = new byte[file.getBytes().length];
		fileName = file.getBytes();

		byte[] readMode = new byte[mode.getBytes().length];
		readMode = mode.getBytes();
		
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		
		try {
			outStream.write(0);
			
			switch (type) {
				case 1: // RRQ
					outStream.write(1);
					break;
				case 2: // WRQ
					outStream.write(2);
					break;
				case 3: // DATA
					outStream.write(3);
					break;
				case 4: // ACK
					outStream.write(4);
					break;
				case 5: // ERR
					outStream.write(5);
					break;
				default:
					break;
			}
			
			outStream.write(fileName);
			outStream.write(0);
			outStream.write(readMode);
			outStream.write(0);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		byte[] data = outStream.toByteArray();
		// Finished creating request packet
		printRequestInformation(data);
		System.out.println("[SYSTEM] Sending request to server at port " + SERVER_PORT + ".");
		
		try {
			sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), SERVER_PORT);
			srSocket.send(sendPacket);
		} 
		catch (UnknownHostException e) {
			e.printStackTrace();
			System.exit(1);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		byte[] buffer = new byte[512];
		receivePacket = new DatagramPacket(buffer, buffer.length);
		
		try {
			System.out.println("[SYSTEM] Waiting for response from server.");
			srSocket.receive(receivePacket);
		}
		catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		data = new byte[receivePacket.getLength()];
		System.arraycopy(receivePacket.getData(), receivePacket.getOffset(), data, 0, receivePacket.getLength());
		printRequestInformation(data);
		System.out.println("[SYSTEM] End of request.");
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
>>>>>>> 7bf250e49f673b724f68d5f91c52670e736044f0
