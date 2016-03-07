package packet;

import java.math.BigInteger;
import java.util.Arrays;

public class Test {
  public static void main(String[] args) {
    
    // test byte to int conversion
    for (int i = 0; i < Math.pow(2, 16); i++) {
      BigInteger bigInt = BigInteger.valueOf(i);      
      byte[] blockNumberBytes = bigInt.toByteArray();
      
      if (blockNumberBytes.length == 1) {
        blockNumberBytes = new byte[]{0, blockNumberBytes[0]};
      }
           
      String[] bitstrings = new String[blockNumberBytes.length];
      for (int j = 0; j < blockNumberBytes.length; j++) {
        bitstrings[j] = String.format("%8s", Integer.toBinaryString(blockNumberBytes[j] & 0xFF)).replace(' ', '0');
      }
      
      System.out.println(Arrays.toString(blockNumberBytes) + " " + Arrays.toString(bitstrings));
      BigInteger bigInt2 = new BigInteger(blockNumberBytes);
      System.out.println(i + " == " + bigInt2.intValue());
      if (i != bigInt2.intValue()) throw new RuntimeException("Wrong byte to integer conversion");
    }
    
    
  }
}
