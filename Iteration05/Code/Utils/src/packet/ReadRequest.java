package packet;


import java.net.InetAddress;
import java.nio.ByteBuffer;

public class ReadRequest extends Request {
  public ReadRequest(InetAddress remoteHost, int remotePort, String filename, String mode) {
    super(remoteHost, remotePort, filename, mode);
  }

  public byte[] getPacketData() {
    int size = 4 + super.filename.length() + super.mode.length();
    ByteBuffer requestBuffer = ByteBuffer.allocate(size);
    
    requestBuffer.put((byte) 0);
    requestBuffer.put((byte) 1);
    requestBuffer.put(super.filename.getBytes());
    requestBuffer.put((byte) 0);
    requestBuffer.put(super.mode.getBytes());
    requestBuffer.put((byte) 0);
    
    return requestBuffer.array();
  }
  
  @Override
  public RequestType type() {
    return RequestType.READ;
  }

  @Override
  public byte[] getOpcode() {
    return new byte[] {0, 1};
  }

  @Override
  public String toString() {
    return "ReadRequest [\n    filename=" + filename + ",\n    mode=" + mode + "\n]";
  }
}
