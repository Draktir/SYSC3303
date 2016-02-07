package packet;


import java.math.BigInteger;
import java.net.DatagramPacket;

public class DataPacketParser {
  public DataPacket parse(DatagramPacket packet) throws InvalidDataPacketException {
    // copy data out of the datagram buffer
    int len = packet.getLength();
    byte[] rawData = new byte[len];
    System.arraycopy(packet.getData(), 0, rawData, 0, len);
    
    if (rawData.length > 516) {
      throw new InvalidDataPacketException("Packet cannot be longer than 516 bytes");
    }
    if (rawData.length < 4) {
      throw new InvalidDataPacketException("Packet must be at least 4 bytes long");
    }
    if (rawData[0] != 0) {
      throw new InvalidDataPacketException("Packet must start with 0 byte");
    }
    if (rawData[1] != 3) {
      throw new InvalidDataPacketException("Second position must be a 3 byte");
    }
    
    byte[] blockNumberBytes = {rawData[2], rawData[3]};
    BigInteger bigInt = new BigInteger(blockNumberBytes);
    int blockNumber = bigInt.intValue();
    
    int fileDataLength = rawData.length - 4;
    byte[] fileData = new byte[fileDataLength];
    
    for (int i = 4; i < rawData.length; i++) {
      fileData[i - 4] = rawData[i];
    }
    
    DataPacketBuilder builder = new DataPacketBuilder();
    builder.setRemoteHost(packet.getAddress());
    builder.setRemotePort(packet.getPort());
    builder.setBlockNumber(blockNumber);
    builder.setFileData(fileData);
    
    return builder.buildDataPacket();
  }
}
