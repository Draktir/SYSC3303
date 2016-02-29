package modification;

public class TidModification {
  private int port;
  
  public TidModification(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public String toString() {
    return "TidModification [port=" + port + "]";
  }
}
