package packet;


import java.math.BigInteger;
import java.net.DatagramPacket;

public class AcknowledgementParser {
  public Acknowledgement parse(DatagramPacket packet) throws InvalidAcknowledgementException {
    // copy data out of the datagram buffer
    int len = packet.getLength();
    byte[] rawData = new byte[len];
    System.arraycopy(packet.getData(), 0, rawData, 0, len);
    
    if (rawData.length != 4) {
      throw new InvalidAcknowledgementException("Malformed Packet: packet is " + len + " bytes long, expected 4 bytes");
    }
    if (rawData[0] != 0) {
      throw new InvalidAcknowledgementException("Malformed packet: first byte is " + rawData[0] + " expected 0");
    }
    if (rawData[1] != 4) {
      throw new InvalidAcknowledgementException("Invalid opcode: got " + rawData[0] + rawData[1] + ", expected 03");
    }
    
    byte[] blockNumberBytes = {rawData[2], rawData[3]};
    BigInteger bigInt = new BigInteger(blockNumberBytes);
    int blockNumber = bigInt.intValue();
    
    AcknowledgementBuilder builder = new AcknowledgementBuilder();
    builder.setRemoteHost(packet.getAddress());
    builder.setRemotePort(packet.getPort());
    builder.setBlockNumber(blockNumber);
    return builder.buildAcknowledgement();
  }
}
