package packet;

import java.util.Arrays;

public class RequestBuilder extends PacketBuilder {
  private String filename = "";
  private String mode = "netascii";
  
  public WriteRequest buildWriteRequest() {
    return new WriteRequest(remoteHost, remotePort, localHost, localPort, filename, mode);
  }
  
  public ReadRequest buildReadRequest() {
    return new ReadRequest(remoteHost, remotePort, localHost, localPort, filename, mode);
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
    this.mode = mode;
  }

  @Override
  public String toString() {
    return "RequestBuilder [filename=" + filename + ", mode=" + mode + ", remoteHost=" + remoteHost + ", remotePort="
        + remotePort + ", localHost=" + localHost + ", localPort=" + localPort + ", packetData="
        + Arrays.toString(packetData) + "]";
  }
}
