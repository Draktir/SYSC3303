package packet;

public class ErrorPacketBuilder extends PacketBuilder<ErrorPacketBuilder> {
  private ErrorPacket.ErrorCode errorCode;
  private String message;
  
  public ErrorPacket buildErrorPacket() {
    return new ErrorPacket(remoteHost, remotePort, errorCode, message);
  }

  public ErrorPacket.ErrorCode getErrorCode() {
    return errorCode;
  }

  public void setErrorCode(ErrorPacket.ErrorCode errorCode) {
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
    return "ErrorPacketBuilder [errorCode=" + errorCode + ", message=" + message + ", remoteHost=" + remoteHost
        + ", remotePort=" + remotePort + "]";
  }
}
