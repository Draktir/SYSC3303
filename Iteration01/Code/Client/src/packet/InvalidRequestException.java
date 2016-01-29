package packet;

public class InvalidRequestException extends Exception {
  private static final long serialVersionUID = 1L;

  public InvalidRequestException(String message) {
      super(message);
  }

  public InvalidRequestException(String message, Throwable throwable) {
      super(message, throwable);
  }
}