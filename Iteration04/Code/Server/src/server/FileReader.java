package server;

import java.io.*;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FileReader {

  // Eclipse doesn't understand Lambdas and Lexical scoping properly
  // so we need to suppress the resource warnings.
  @SuppressWarnings("resource")
  
  // returns a lazy stream of blocks (byte[]) read from the file
  public Stream<byte[]> read(final String filename, final int blockSize)
        throws FileNotFoundException, AccessDeniedException {

    Path path = Paths.get(filename);
    File file = path.toFile();

    if (!file.exists()) {
      throw new FileNotFoundException("File does not exist.");
    }

    if (!file.isFile()) {
      throw new FileNotFoundException("Not a file.");
    }

    if (file.length() == 0) {
      throw new FileNotFoundException("File is empty.");
    }

    if (!file.canRead()) {
      throw new AccessDeniedException("File cannot be read.");
    }

    BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(filename));

    Supplier<byte[]> readBlock = () -> {
      byte[] buffer = new byte[blockSize];
      int bytesRead;
      try {
        bytesRead = fileIn.read(buffer);
      } catch (IOException e) {
        return new byte[0];
      }
      
      if (bytesRead < 0) {
        return new byte[0];
      }
      
      byte[] data = new byte[bytesRead];
      System.arraycopy(buffer, 0, data, 0, bytesRead);
      return data;
    };

    double size = 0.0;
    try {
      size = Files.size(path);
    } catch (IOException e) {
      throw new AccessDeniedException("Cannot access file.");
    }
    double blocks = Math.ceil(size / (double) blockSize);

    return Stream
        .generate(readBlock)
        .limit((long) blocks)
        .onClose(() -> {
          // this gets called on Stream.close()
          try {
            fileIn.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
        });
  }
}
