package packet;


import java.math.BigInteger;

public class AcknowledgementParser {
  public Acknowledgement parse(Packet packet) throws InvalidDataPacketException {
    byte[] rawData = packet.getPacketData();
    
    if (rawData.length != 4) {
      throw new InvalidDataPacketException("Packet must be exactly 4 bytes long");
    }
    if (rawData[0] != 0) {
      throw new InvalidDataPacketException("Packet must start with a 0 byte");
    }
    if (rawData[1] != 4) {
      throw new InvalidDataPacketException("Second position must be a 4 byte");
    }
    
    byte[] blockNumberBytes = {rawData[2], rawData[3]};
    BigInteger bigInt = new BigInteger(blockNumberBytes);
    int blockNumber = bigInt.intValue();
    
    AcknowledgementBuilder builder = new AcknowledgementBuilder();
    builder.setRemoteHost(packet.getRemoteHost());
    builder.setRemotePort(packet.getRemotePort());
    builder.setBlockNumber(blockNumber);
    return builder.buildAcknowledgement();
  }
}
