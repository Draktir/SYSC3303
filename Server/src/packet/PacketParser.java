package packet;

import packet.Packet;
import request.RequestParser;

public class PacketParser {
  static final int BUFFER_SIZE = 516;
  
  public Packet parse(Packet packet) throws InvalidPacketException {
    byte[] rawRequest = packet.getPacketData();
    
    if (rawRequest[0] != 0) {
      throw new InvalidPacketException("First byte must be 0.");
    }
    
    try {
      switch (rawRequest[1]) {
        case 1:
        case 2:
          RequestParser rParser = new RequestParser();
          return rParser.parse(packet);
        case 3:
          DataPacketParser dParser = new DataPacketParser();
          return dParser.parse(packet);
        case 4:
          AcknowledgementParser aParser = new AcknowledgementParser();
          return aParser.parse(packet);
        case 5:
          System.out.println("Error packets not yet implented");
        default:
          throw new InvalidPacketException("Cannot identify packet type");
      }  
    } catch (Exception e) {
      throw new InvalidPacketException(e.getMessage());
    }
  }
}
