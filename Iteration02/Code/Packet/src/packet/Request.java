package packet;


import java.net.InetAddress;

public abstract class Request extends Packet {
  protected String filename;
  protected String mode;
  
  public Request(InetAddress remoteHost, int remotePort, String filename, String mode) {
    super(remoteHost, remotePort);
    this.filename = filename;
    
    if (!mode.equalsIgnoreCase("octet") && !mode.equalsIgnoreCase("netascii")) {
      throw new RuntimeException("Invalid mode (must 'octet' or 'netascii'): " + mode);
    }
    this.mode = mode.toLowerCase();
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
