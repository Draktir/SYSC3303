package packet;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;

import packet.ErrorPacket.ErrorCode;

public class ErrorPacketParser {
  private static final int STR_BUFFER_SIZE = 511; // 516 - 5
  
  public ErrorPacket parse(DatagramPacket packet) throws InvalidErrorPacketException {
    // copy data out of the datagram buffer
    int len = packet.getLength();
    byte[] rawData = new byte[len];
    System.arraycopy(packet.getData(), 0, rawData, 0, len);
    
    if (rawData.length < 5) {
      throw new InvalidErrorPacketException("Malformed Packet: packet is " + len + " bytes long, min: 5 bytes");
    }
    if (rawData[0] != 0) {
      throw new InvalidErrorPacketException("Malformed packet: first byte is " + rawData[0] + " expected 0");
    }
    if (rawData[1] != 5) {
      throw new InvalidErrorPacketException("Invalid opcode: got 0" + rawData[1] + ", expected 05");
    }
    
    byte[] blockNumberBytes = {rawData[2], rawData[3]};
    BigInteger bigInt = new BigInteger(blockNumberBytes);
    int errorCodeInt = bigInt.intValue();
    
    ErrorCode errorCode = ErrorCode.fromValue(errorCodeInt);
    if (errorCode == null) {
      throw new InvalidErrorPacketException("Invalid error code: " + rawData[2] + "" + rawData[3]);
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
    builder.setRemoteHost(packet.getAddress());
    builder.setRemotePort(packet.getPort());
    
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
