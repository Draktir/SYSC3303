package packet;

import java.nio.ByteBuffer;

public class RequestParser {
  static final int BUFFER_SIZE = 100;
  
  public Request parse(Packet packet) throws InvalidRequestException {
    byte[] rawRequest = packet.getPacketData();
    
    if (rawRequest[0] != 0) {
      throw new InvalidRequestException("First byte must be 0.");
    }
    
    byte requestType = rawRequest[1];
    if (requestType != 1 && requestType != 2) {
      throw new InvalidRequestException("Second byte must be 1 or 2.");
    }
    
    int currentOffset = 2;
    
    String filename = extractString(rawRequest, currentOffset);
    currentOffset += filename.length() + 1;
    
    String mode = extractString(rawRequest, currentOffset);
    currentOffset += mode.length() + 1;
    
    if (!mode.equalsIgnoreCase("octet") && !mode.equalsIgnoreCase("netascii")) {
      throw new InvalidRequestException("The mode must be either 'octet' or 'netascii'");
    }
    
    if (rawRequest.length > (currentOffset + 1)) {
      throw new InvalidRequestException("The request must be complete after the third 0 byte");
    }
    
    // Message is valid, build request object
    RequestBuilder builder = new RequestBuilder();
    builder.setFilename(filename);
    builder.setMode(mode);
    builder.setLocalHost(packet.getLocalHost());
    builder.setLocalPort(packet.getLocalPort());
    builder.setRemoteHost(packet.getRemoteHost());
    builder.setRemotePort(packet.getRemotePort());
    
    if (requestType == 1) {
      return builder.buildReadRequest();
    } else if (requestType == 2) {
      return builder.buildWriteRequest();
    } else {
      return null;
    }
  }
  
  private String extractString(byte[] msg, int offset) throws InvalidRequestException {
    ByteBuffer stringBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    int i = offset;
    
    while (msg[i] != 0 && i < msg.length && (i - offset) < BUFFER_SIZE) {
      stringBuffer.put(msg[i]);
      i++;
    }
    
    if ((i - offset) >= BUFFER_SIZE) {
      throw new InvalidRequestException("String must be terminated by 0 byte.");
    }
    
    return new String(stringBuffer.array()).trim();
  }
}
