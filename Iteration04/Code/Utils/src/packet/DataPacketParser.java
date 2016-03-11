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
      throw new InvalidDataPacketException("Malformed Packet: packet is " + len + " bytes long, max: 516 bytes");
    }
    if (rawData.length < 4) {
      throw new InvalidDataPacketException("Malformed Packet: packet is " + len + " bytes long, min: 4 bytes");
    }
    if (rawData[0] != 0) {
      throw new InvalidDataPacketException("Malformed packet: first byte is " + rawData[0] + " expected 0");
    }
    if (rawData[1] != 3) {
      throw new InvalidDataPacketException("Invalid opcode: got " + rawData[0] + rawData[1] + ", expected 03");
    }
    
    byte[] blockNumberBytes = {rawData[2], rawData[3]};
    BigInteger bigInt = new BigInteger(blockNumberBytes);
    int blockNumber = bigInt.intValue();
    
    if (blockNumber <= 0) {
      throw new InvalidDataPacketException("Invalid block number: " + blockNumber + ", must be greater than 0");
    }
    
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
