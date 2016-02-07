package packet;


public class InvalidErrorPacketException extends Exception {
  private static final long serialVersionUID = 1L;

  public InvalidErrorPacketException(String message) {
      super(message);
  }

  public InvalidErrorPacketException(String message, Throwable throwable) {
      super(message, throwable);
  }
}