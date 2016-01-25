package packet;

import java.net.InetAddress;
import java.net.UnknownHostException;

import utils.Formatter;

public class Test {

  public static void main(String[] args) {
    InetAddress localhost = null;
    try {
      localhost = InetAddress.getLocalHost();
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
    
    byte[] fileData = {1, 22, 32, 10, 120, 2, 11, 9, 22};
    
    DataPacket data = new DataPacket(localhost, 68, localhost, 68, 0, fileData);
    DataPacketParser parser = new DataPacketParser();
    
    for (int i = 0; i <= 1100; i += 50) {
      data.setBlockNumber(i);
      System.out.println(Formatter.byteArray(data.getPacketData()));
      try {
        System.out.println(parser.parse(data).toString());
      } catch (InvalidDataPacketException e) {
        e.printStackTrace();
      }
    }
    
  }

}
