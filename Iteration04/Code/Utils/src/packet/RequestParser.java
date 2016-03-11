package packet;


import java.net.DatagramPacket;
import java.nio.ByteBuffer;

public class RequestParser {
  static final int STR_BUFFER_SIZE = 256; // (516 - 4) / 2
  
  public Request parse(DatagramPacket packet) throws InvalidRequestException {
    // copy data out of the datagram buffer
    int len = packet.getLength();
    byte[] rawRequest = new byte[len];
    System.arraycopy(packet.getData(), 0, rawRequest, 0, len);
    
    if (rawRequest[0] != 0) {
      throw new InvalidRequestException("Malformed packet: First byte is " + rawRequest[0] + " expected 0.");
    }
    
    byte requestType = rawRequest[1];
    if (requestType != 1 && requestType != 2) {
      throw new InvalidRequestException("Not a valid request opcode. Got " + rawRequest[0] + rawRequest[1]
          + ", expected 01 or 02");
    }
    
    int currentOffset = 2;
    String filename = extractString(rawRequest, currentOffset);
    if (filename.length() <= 0) {
      throw new InvalidRequestException("Invalid filename: cannot be of length 0");
    }
    currentOffset += filename.length() + 1;
    
    String mode = extractString(rawRequest, currentOffset);   
    if (!mode.equalsIgnoreCase("octet") && !mode.equalsIgnoreCase("netascii")) {
      throw new InvalidRequestException("Invalid mode: must be either 'octet' or 'netascii'");
    }
    currentOffset += mode.length() + 1;
    
    if (rawRequest.length > (currentOffset + 1)) {
      throw new InvalidRequestException("Malformed packet: must complete after the third 0 byte");
    }
    
    // Packet is valid, build request object
    RequestBuilder builder = new RequestBuilder();
    builder.setFilename(filename);
    builder.setMode(mode);
    builder.setRemoteHost(packet.getAddress());
    builder.setRemotePort(packet.getPort());
    
    if (requestType == 1) {
      return builder.buildReadRequest();
    } else if (requestType == 2) {
      return builder.buildWriteRequest();
    } else {
      return null;
    }
  }
  
  private String extractString(byte[] msg, int offset) throws InvalidRequestException {
    ByteBuffer stringBuffer = ByteBuffer.allocate(STR_BUFFER_SIZE);
    int i = offset;
    
    while (msg[i] != 0 && i < msg.length && (i - offset) < STR_BUFFER_SIZE) {
      stringBuffer.put(msg[i]);
      i++;
    }
    
    if ((i - offset) >= STR_BUFFER_SIZE || msg[i] != 0) {
      throw new InvalidRequestException("String must be terminated by 0 byte.");
    }
    
    return new String(stringBuffer.array()).trim();
  }
}
