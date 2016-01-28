package server;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileWriter {
  private String filename;
  private BufferedOutputStream fileOut;
  
  public FileWriter(String filename) throws FileNotFoundException {
    this.filename = filename;
    fileOut = new BufferedOutputStream(new FileOutputStream(filename));
  }
  
  public boolean writeBlock(byte[] data) {
    try {
      fileOut.write(data, 0, data.length);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
  
  public void close() {
    try {
      this.fileOut.close();
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
    return "FileWriter [filename=" + filename + ", fileOut=" + fileOut + "]";
  }
}
