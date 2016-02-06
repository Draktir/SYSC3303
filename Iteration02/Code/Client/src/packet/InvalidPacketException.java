package packet;

public class InvalidPacketException extends Exception {
  private static final long serialVersionUID = 1L;

  public InvalidPacketException(String message) {
      super(message);
  }

  public InvalidPacketException(String message, Throwable throwable) {
      super(message, throwable);
  }
}