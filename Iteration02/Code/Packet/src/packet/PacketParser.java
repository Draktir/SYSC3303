package packet;


import java.net.DatagramPacket;

import packet.Packet;

public class PacketParser {
  public Packet parse(DatagramPacket datagramPacket) throws InvalidPacketException {
    if (datagramPacket.getLength() < 2) {
      throw new InvalidDataPacketException("Packet is too short to make any sense.");
    }
    byte[] data = datagramPacket.getData();
    
    if (data[0] != 0) {
      throw new InvalidPacketException("First byte must be 0.");
    }
    
    try {
      switch (data[1]) {
        case 1:
        case 2:
          RequestParser rParser = new RequestParser();
          return rParser.parse(datagramPacket);
        case 3:
          DataPacketParser dParser = new DataPacketParser();
          return dParser.parse(datagramPacket);
        case 4:
          AcknowledgementParser aParser = new AcknowledgementParser();
          return aParser.parse(datagramPacket);
        case 5:
          ErrorPacketParser eParser = new ErrorPacketParser();
          return eParser.parse(datagramPacket);
        default:
          throw new InvalidPacketException("Cannot identify packet type");
      }  
    } catch (Exception e) {
      throw new InvalidPacketException(e.getMessage());
    }
  }
  
  public Request parseRequest(DatagramPacket datagramPacket) throws InvalidRequestException {
    RequestParser rParser = new RequestParser();
    return rParser.parse(datagramPacket);
  }
  
  public DataPacket parseDataPacket(DatagramPacket datagramPacket) throws InvalidDataPacketException {
    DataPacketParser dParser = new DataPacketParser();
    return dParser.parse(datagramPacket);
  }
  
  public Acknowledgement parseAcknowledgement(DatagramPacket datagramPacket) throws InvalidAcknowledgementException {
    AcknowledgementParser aParser = new AcknowledgementParser();
    return aParser.parse(datagramPacket);
  }
  
  public ErrorPacket parseErrorPacket(DatagramPacket datagramPacket) throws InvalidErrorPacketException {
    ErrorPacketParser eParser = new ErrorPacketParser();
    return eParser.parse(datagramPacket);
  }
}
