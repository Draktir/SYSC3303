package server;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileReader {
  private String filename;
  private BufferedInputStream fileIn;
    
  public FileReader(String filename) throws FileNotFoundException {
    this.setFilename(filename);
    fileIn = new BufferedInputStream(new FileInputStream(filename));
  }
  
  /**
   * Reads buffer.length bytes from the file into a buffer.
   * @param buffer
   * @return number of bytes read, or -1 on EOF or error.
   */
  public int readBlock(byte[] buffer) {
    try {
      return fileIn.read(buffer);
    } catch (IOException e) {
      e.printStackTrace();
      return -1;
    }
  }
  
  public void close() {
    try {
      fileIn.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public String toString() {
    return "FileReader [filename=" + filename + ", fileIn=" + fileIn + "]";
  }
}
