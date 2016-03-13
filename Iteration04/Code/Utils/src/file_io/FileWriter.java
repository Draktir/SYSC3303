package file_io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;

public class FileWriter {
  private String filename;
  private BufferedOutputStream fileOut = null;

  public FileWriter(String filename) {
    this.filename = filename;
  }
  
  
  /**
   * Creates the file for this instance. Should be called before trying
   * to write anything.
   * 
   * @throws FileAlreadyExistsException
   * @throws AccessDeniedException
   */
  public void createFile() throws FileAlreadyExistsException, AccessDeniedException {
    File file = new File(filename);

    if (file.exists()) {
      throw new FileAlreadyExistsException(filename + " already exists.");
    }
    
    try {
      file.createNewFile();
    } catch (IOException e) {
      throw new AccessDeniedException("Could not create file " + filename);
    }
    
    // this is highly unlikely since we were able to create the file,
    // but doesn't hurt to check.
    if (!file.canWrite()) {
      file.delete();
      throw new AccessDeniedException("Insufficient permission to write to " + filename);
    }
  }
  
  /**
   * Writes a block of data to the file
   * 
   * @param data
   * @throws IOException 
   */
  public void writeBlock(byte[] data) throws IOException {    
    // we're lazy, don't create the file stream until we have to
    if (fileOut == null) {
      fileOut = new BufferedOutputStream(new FileOutputStream(filename));
    }
    fileOut.write(data, 0, data.length);
  }
  
  /**
   * Closes the file output stream
   * 
   */
  public void close() {
    if (this.fileOut == null) {
      return; 
    }
    
    try {
      this.fileOut.flush();
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
