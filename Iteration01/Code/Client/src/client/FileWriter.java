package client;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileWriter {
  private String filename;
  private BufferedOutputStream fileOut;
  
  /**
   * Constructor, accepts filename of file to be written
   * 
   * @param filename
   * @throws FileNotFoundException
   */
  public FileWriter(String filename) throws FileNotFoundException {
    this.filename = filename;
    
    // create the file
    // TODO: Throw an exception if the file already exists
    File f = new File(filename);
    try {
      f.createNewFile();
    } catch (IOException e) {
      e.printStackTrace();
    }
    
    fileOut = new BufferedOutputStream(new FileOutputStream(filename));
  }
  
  /**
   * Writes a block of data to the file
   * 
   * @param data
   * @return true on success / false on failure
   */
  public boolean writeBlock(byte[] data) {
    try {
      fileOut.write(data, 0, data.length);
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
  
  /**
   * Closes the file output stream
   * 
   */
  public void close() {
    try {
      this.fileOut.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Accessor for filename attribute
   * 
   * @return filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Mutator for filename
   * 
   * @param filename
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }

  /**
   * Generic toString method
   */
  @Override
  public String toString() {
    return "FileWriter [filename=" + filename + ", fileOut=" + fileOut + "]";
  }
}
