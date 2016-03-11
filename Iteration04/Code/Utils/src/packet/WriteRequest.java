package packet;

import java.net.InetAddress;
import java.nio.ByteBuffer;

public class WriteRequest extends Request {

  public WriteRequest(InetAddress remoteHost, int remotePort, String filename, String mode) {
    super(remoteHost, remotePort, filename, mode);
  }

  @Override
  public byte[] getPacketData() {
    int size = 4 + filename.length() + mode.length();
    ByteBuffer requestBuffer = ByteBuffer.allocate(size);
    
    requestBuffer.put((byte) 0);
    requestBuffer.put((byte) 2);
    requestBuffer.put(filename.getBytes());
    requestBuffer.put((byte) 0);
    requestBuffer.put(mode.getBytes());
    requestBuffer.put((byte) 0);
    
    return requestBuffer.array();
  }
  
  @Override
  public RequestType type() {
    return RequestType.WRITE;
  }
  
  @Override
  public byte[] getOpcode() {
    return new byte[] {0, 2};
  }
  
  @Override
  public String toString() {
    return "WriteRequest [\n    filename=" + filename + ",\n    mode=" + mode + ",\n    remoteHost=" + remoteHost
        + ",\n    remotePort=" + remotePort + "\n]";
  }
}
