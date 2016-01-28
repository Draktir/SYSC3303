package packet;

import java.nio.ByteBuffer;

public class AcknowledgementParser {
  public Acknowledgement parse(Packet packet) throws InvalidDataPacketException {
    byte[] rawData = packet.getPacketData();
    
    if (rawData.length < 4) {
      throw new InvalidDataPacketException("Packet must be at least 4 bytes long");
    }
    if (rawData[0] != 0) {
      throw new InvalidDataPacketException("Packet must start with 0 byte");
    }
    if (rawData[1] != 4) {
      throw new InvalidDataPacketException("Second position must be a 3 byte");
    }
    
    byte[] blockNumberBytes = {0, 0, rawData[2], rawData[3]};
    int blockNumber = ByteBuffer.wrap(blockNumberBytes).getInt();
    
    AcknowledgementBuilder builder = new AcknowledgementBuilder();
    builder.setRemoteHost(packet.getRemoteHost());
    builder.setRemotePort(packet.getRemotePort());
    builder.setBlockNumber(blockNumber);
    return builder.buildAcknowledgement();
  }
}
