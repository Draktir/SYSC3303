package file_io;

import java.io.IOException;

public class DiskFullException extends IOException {
  private static final long serialVersionUID = 1L;

  public DiskFullException(String message) {
      super(message);
  }

  public DiskFullException(String message, Throwable throwable) {
      super(message, throwable);
  }

}