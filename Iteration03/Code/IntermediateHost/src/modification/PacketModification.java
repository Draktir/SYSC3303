package modification;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import packet.ErrorPacket;
import packet.InvalidErrorPacketException;
import packet.Packet;
import packet.PacketParser;

public abstract class PacketModification {
  protected int packetNumber;
  protected byte[] appendToEnd = null;
  protected TidModification tidModification = null;
  protected DelayPacketModification delayModification = null;
  protected DropPacketModification dropModification = null;

  public PacketModification(int packetNumber) {
    this.packetNumber = packetNumber;
  }

  public abstract byte[] apply(Packet packet, int localReceivePort, int remoteReceivePort, Consumer<Packet> delayedPacketConsumer);
  
  public void performTidModification(Packet packet, int remoteReceivePort) {
    System.out.println("[Modification] Sending packet with wrong TID: port " + tidModification.getPort());
    DatagramSocket tempSock = null;
    try {
      tempSock = new DatagramSocket(tidModification.getPort());
    } catch (SocketException e) {
      e.printStackTrace();
      return;
    }
    
    byte[] data = packet.getPacketData();
    DatagramPacket sendDatagram = new DatagramPacket(data, data.length,
        packet.getRemoteHost(), remoteReceivePort);
    
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
      System.err.println("[Modification] Did not receive an error packet. Expected an error, code 5.");
      tempSock.close();
      return;
    }
    
    System.out.println("[Modification] Error packet received: \n" + errPacket.toString() + "\n");
    tempSock.close();
  }
  
  public void performDelayPacketModification(Packet packet, int localReceivePort, Consumer<Packet> delayedPacketConsumer) {
    /*
     * Start a new thread that will hold on to the packet and sleep for a while.
     * Then call the delayed packet consumer.
     * 
     * N.B. I'll have to test this on the lab computers. Java 8's Lambdas may
     * not work with that version of Eclipse.
     */
    
    int delay = this.delayModification.getDelay();
    
    Runnable delayTask = () -> {
      System.out.println("[Modification] Delaying packet for " + delay + "s: " + packet.toString());
      try {
        Thread.sleep(delay * 1000);
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
      
      System.out.println("[Modification] Re-sending delayed packet: " + packet.toString());
      delayedPacketConsumer.accept(packet);
    };
    
    new Thread(delayTask).start();
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

  public DelayPacketModification getDelayModification() {
    return delayModification;
  }

  public void setDelayModification(DelayPacketModification delayModification) {
    this.delayModification = delayModification;
  }

  public DropPacketModification getDropModification() {
    return dropModification;
  }

  public void setDropModification(DropPacketModification dropModification) {
    this.dropModification = dropModification;
  }
 
  @Override
  public String toString() {
    return "PacketModification [\n    packetNumber=" + packetNumber + ",\n    appendToEnd="
        + Arrays.toString(appendToEnd) + ",\n    tidModification=" + tidModification + ",\n    delayModification="
        + delayModification + ",\n    dropModification=" + dropModification + "\n]";
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
