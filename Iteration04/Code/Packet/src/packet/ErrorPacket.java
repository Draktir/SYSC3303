package packet;

import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.ByteBuffer;

public class ErrorPacket extends Packet {
   
  public enum ErrorCode {
    NOT_DEFINED(0),
    FILE_NOT_FOUND(1),
    ACCESS_VIOLATION(2),
    DISK_FULL_OR_ALLOCATION_EXCEEDED(3),
    ILLEGAL_TFTP_OPERATION(4),
    UNKNOWN_TRANSFER_ID(5),
    FILE_ALREADY_EXISTS(6),
    NO_SUCH_USER(7);
    
    private final int value;
    private ErrorCode(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
    
    public static ErrorCode fromValue(int value) {
      ErrorCode[] errCodes = ErrorCode.values();
      return value > errCodes.length ? null : errCodes[value];
    }
  }
  
  private ErrorCode errorCode;
  private String message;
  
  public ErrorPacket(InetAddress remoteHost, int remotePort, ErrorCode errorCode, String message) {
    super(remoteHost, remotePort);
    this.errorCode = errorCode;
    this.message = message;
  }

  @Override
  public byte[] getPacketData() {
    // convert error code from int to byte array
    BigInteger bigInt = BigInteger.valueOf(errorCode.getValue());      
    byte[] errorCodeBytes = bigInt.toByteArray();
    if (errorCodeBytes.length == 1) {
      errorCodeBytes = new byte[]{0, errorCodeBytes[0]};
    }
    
    ByteBuffer packetBuffer = ByteBuffer.allocate(5 + message.length());
    packetBuffer.put((byte) 0);
    packetBuffer.put((byte) 5);
    packetBuffer.put(errorCodeBytes[0]);
    packetBuffer.put(errorCodeBytes[1]);
    packetBuffer.put(message.getBytes());
    packetBuffer.put((byte) 0);
    
    return packetBuffer.array();
  }
  
  @Override
  public byte[] getOpcode() {
    return new byte[] {0, 5};
  }

  public ErrorCode getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return "ErrorPacket [\n    errorCode=" + errorCode + ",\n    message=" + message + ",\n    remoteHost=" + remoteHost
        + ",\n    remotePort=" + remotePort + "\n]";
  }
}
