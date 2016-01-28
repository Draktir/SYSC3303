package packet;

import java.net.InetAddress;

public abstract class Request extends Packet {
  protected String filename;
  protected String mode;
  
  public Request(InetAddress remoteHost, int remotePort, InetAddress localHost, int localPort, 
      String filename, String mode) {
    super(remoteHost, remotePort);
    this.filename = filename;
    this.mode = (mode != null) ? mode.toLowerCase() : "netascii";
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getMode() {
    return mode;
  }

  public void setMode(String mode) {
    this.mode = (mode != null) ? mode.toLowerCase() : "netascii";
  }

  @Override
  public String toString() {
    return "Request [filename=" + filename + ", mode=" + mode + ", remoteHost=" + remoteHost + ", remotePort="
        + remotePort + "]";
  }
}
