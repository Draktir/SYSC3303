package packet;


import java.net.DatagramPacket;

import packet.Packet;

public class PacketParser {
  RequestParser requestParser = new RequestParser();
  DataPacketParser dataPacketParser = new DataPacketParser();
  AcknowledgementParser acknowledgementParser = new AcknowledgementParser();
  ErrorPacketParser errorPacketParser = new ErrorPacketParser();
  
  public Packet parse(DatagramPacket datagramPacket) throws InvalidPacketException {
    if (datagramPacket.getLength() < 2) {
      throw new InvalidDataPacketException("Packet length < 2 is too short to make any sense.");
    }
    byte[] data = datagramPacket.getData();
    
    if (data[0] != 0) {
      throw new InvalidPacketException("Malformed packet: first byte is " + data[0] + "expected 0");
    }
    
    switch (data[1]) {
      case 1:
      case 2:
        return requestParser.parse(datagramPacket);
      case 3:
        return dataPacketParser.parse(datagramPacket);
      case 4:
        return acknowledgementParser.parse(datagramPacket);
      case 5:
        return errorPacketParser.parse(datagramPacket);
      default:
        throw new InvalidPacketException("Invalid opcode 0" + data[1]);
    }  
  }
  
  public Request parseRequest(DatagramPacket datagramPacket) throws InvalidRequestException {
    return requestParser.parse(datagramPacket);
  }
  
  public DataPacket parseDataPacket(DatagramPacket datagramPacket) throws InvalidDataPacketException {
    return dataPacketParser.parse(datagramPacket);
  }
  
  public Acknowledgement parseAcknowledgement(DatagramPacket datagramPacket) throws InvalidAcknowledgementException {
    return acknowledgementParser.parse(datagramPacket);
  }
  
  public ErrorPacket parseErrorPacket(DatagramPacket datagramPacket) throws InvalidErrorPacketException {
    return errorPacketParser.parse(datagramPacket);
  }
}
