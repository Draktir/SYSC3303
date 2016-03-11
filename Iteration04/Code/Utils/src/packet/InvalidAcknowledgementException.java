package packet;

public class InvalidAcknowledgementException extends InvalidPacketException {
  private static final long serialVersionUID = 1L;

  public InvalidAcknowledgementException(String message) {
      super(message);
  }

  public InvalidAcknowledgementException(String message, Throwable throwable) {
      super(message, throwable);
  }

}