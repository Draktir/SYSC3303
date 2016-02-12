package modification;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;

import packet.ErrorPacket;
import packet.InvalidErrorPacketException;
import packet.Packet;
import packet.PacketParser;

public abstract class PacketModification {
  protected int packetNumber;
  protected byte[] appendToEnd = null;
  protected TidModification tidModification = null; 

  public PacketModification(int packetNumber) {
    this.packetNumber = packetNumber;
  }

  public abstract byte[] apply(Packet packet);
  
  public void performTidModification(Packet packet) {
    System.out.println("Sending packet with wrong TID: port " + tidModification.getPort());
    DatagramSocket tempSock = null;
    try {
      tempSock = new DatagramSocket(tidModification.getPort());
    } catch (SocketException e) {
      e.printStackTrace();
      return;
    }
    
    byte[] data = packet.getPacketData();
    DatagramPacket sendDatagram = new DatagramPacket(data, data.length,
        packet.getRemoteHost(), packet.getRemotePort());
    
    try {
      tempSock.send(sendDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      tempSock.close();
      return;
    }
    
    byte[] buffer = new byte[1024];
    DatagramPacket receiveDatagram = new DatagramPacket(buffer, buffer.length);
    
    try {
      tempSock.receive(receiveDatagram);
    } catch (IOException e) {
      e.printStackTrace();
      tempSock.close();
      return;
    }
    
    PacketParser parser = new PacketParser();
    ErrorPacket errPacket = null;
    try {
      errPacket = parser.parseErrorPacket(receiveDatagram);
    } catch (InvalidErrorPacketException e) {
      e.printStackTrace();
      System.err.println("Did not receive an error packet. Expected an error, code 5.");
      tempSock.close();
      return;
    }
    
    System.out.println("Error packet received: " + errPacket.toString());
    tempSock.close();
  }
  
  public int getPacketNumber() {
    return packetNumber;
  }

  public void setPacketNumber(int packetNumber) {
    this.packetNumber = packetNumber;
  }
  
  public byte[] getAppendToEnd() {
    return appendToEnd;
  }

  public void setAppendToEnd(byte[] appendToEnd) {
    this.appendToEnd = appendToEnd;
  }

  public TidModification getTidModification() {
    return tidModification;
  }

  public void setTidModification(TidModification tidModification) {
    this.tidModification = tidModification;
  }

  @Override
  public String toString() {
    return "TidModification [packetNumber=" + packetNumber + "]";
  }
  
  public static List<Byte> byteArrayToList(byte[] arr) {
    Byte[] bytes = new Byte[arr.length];
    for (int i = 0; i < arr.length; i++) {
      bytes[i] = new Byte(arr[i]);
    }
    return Arrays.asList(bytes);
  }
  
  public static byte[] byteListToArray(List<Byte> list) {
    byte[] arr = new byte[list.size()];
    for (int i = 0; i < list.size(); i++) {
      arr[i] = list.get(i).byteValue();
    }
    return arr;
  }
}
