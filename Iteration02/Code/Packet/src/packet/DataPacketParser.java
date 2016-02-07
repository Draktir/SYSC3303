package packet;


import java.math.BigInteger;

public class DataPacketParser {
  public DataPacket parse(Packet packet) throws InvalidDataPacketException {
    byte[] rawData = packet.getPacketData();
    
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
    BigInteger bigInt2 = new BigInteger(blockNumberBytes);
    int blockNumber = bigInt2.intValue();
    
    int fileDataLength = rawData.length - 4;
    byte[] fileData = new byte[fileDataLength];
    
    for (int i = 4; i < rawData.length; i++) {
      fileData[i - 4] = rawData[i];
    }
    
    DataPacketBuilder builder = new DataPacketBuilder();
    builder.setRemoteHost(packet.getRemoteHost());
    builder.setRemotePort(packet.getRemotePort());
    builder.setBlockNumber(blockNumber);
    builder.setFileData(fileData);
    
    return builder.buildDataPacket();
  }
}
