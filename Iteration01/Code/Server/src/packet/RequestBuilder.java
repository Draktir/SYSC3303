package packet;

import java.util.Arrays;

public class RequestBuilder extends PacketBuilder {
  private String filename = "";
  private String mode = "netascii";
  
  public WriteRequest buildWriteRequest() {
    return new WriteRequest(remoteHost, remotePort, filename, mode);
  }
  
  public ReadRequest buildReadRequest() {
    return new ReadRequest(remoteHost, remotePort, filename, mode);
  }

  public String getFilename() {
    return filename;
  }

  public RequestBuilder setFilename(String filename) {
    this.filename = filename;
    return this;
  }

  public String getMode() {
    return mode;
  }

  public RequestBuilder setMode(String mode) {
    this.mode = mode;
    return this;
  }

  @Override
  public String toString() {
    return "RequestBuilder [filename=" + filename + ", mode=" + mode + ", remoteHost=" + remoteHost + ", remotePort="
        + remotePort + ", packetData=" + Arrays.toString(packetData) + "]";
  }
}
