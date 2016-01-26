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
