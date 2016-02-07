package packet;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import packet.ErrorPacket.ErrorCode;

public class ErrorPacketParser {
  private static final int STR_BUFFER_SIZE = 511; // 516 - 5
  
  public ErrorPacket parse(Packet packet) throws InvalidErrorPacketException {
    byte[] rawData = packet.getPacketData();
    
    if (rawData.length < 5) {
      throw new InvalidErrorPacketException("Packet must be at least 5 bytes long.");
    }
    if (rawData[0] != 0) {
      throw new InvalidErrorPacketException("First byte must be 0.");
    }
    if (rawData[1] != 5) {
      throw new InvalidErrorPacketException("Second byte must be 5.");
    }
    
    byte[] blockNumberBytes = {rawData[2], rawData[3]};
    BigInteger bigInt = new BigInteger(blockNumberBytes);
    int errorCodeInt = bigInt.intValue();
    
    ErrorCode errorCode = ErrorCode.fromValue(errorCodeInt);
    if (errorCode == null) {
      throw new InvalidErrorPacketException("Invalid error code: " + errorCodeInt);
    }
    
    int currentOffset = 4;
    String message = extractString(rawData, currentOffset);
    currentOffset += message.length() + 1;
    
    if (rawData.length > (currentOffset + 1)) {
      throw new InvalidErrorPacketException("The request must be complete after the third 0 byte");
    }
    
    // Packet is valid, build request object
    ErrorPacketBuilder builder = new ErrorPacketBuilder();
    builder.setErrorCode(errorCode);
    builder.setMessage(message);
    builder.setRemoteHost(packet.getRemoteHost());
    builder.setRemotePort(packet.getRemotePort());
    
    return builder.buildErrorPacket();
  }  
  
  private String extractString(byte[] msg, int offset) throws InvalidErrorPacketException {
    ByteBuffer stringBuffer = ByteBuffer.allocate(STR_BUFFER_SIZE);
    int i = offset;
    
    while (msg[i] != 0 && i < msg.length && (i - offset) < STR_BUFFER_SIZE) {
      stringBuffer.put(msg[i]);
      i++;
    }
    
    if ((i - offset) >= STR_BUFFER_SIZE || msg[i] != 0) {
      throw new InvalidErrorPacketException("String must be terminated by 0 byte.");
    }
    
    return new String(stringBuffer.array()).trim();
  }
}
