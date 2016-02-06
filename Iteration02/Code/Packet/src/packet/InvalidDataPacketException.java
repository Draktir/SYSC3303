package packet;


public class InvalidDataPacketException extends Exception {
  private static final long serialVersionUID = 1L;

  public InvalidDataPacketException(String message) {
      super(message);
  }

  public InvalidDataPacketException(String message, Throwable throwable) {
      super(message, throwable);
  }

}