package packet;


public class InvalidRequestException extends InvalidPacketException {
  private static final long serialVersionUID = 1L;

  public InvalidRequestException(String message) {
      super(message);
  }

  public InvalidRequestException(String message, Throwable throwable) {
      super(message, throwable);
  }
}