package file_io;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;

public class FileWriter {
  private String filename;
  private BufferedOutputStream fileOut;
  
  /**
   * Constructor, accepts filename of file to be written
   * 
   * @param filename
   * @throws IOException 
   * @throws FileAlreadyExistsException
   */
  public FileWriter(String filename) throws FileAlreadyExistsException, IOException {
    this.filename = filename;
    
    File f = new File(filename);
    
    
    // TODO: For this iteration simply delete the file if it exists
    // for the next iteration we need to send an error in this case
    if (f.exists()) {
      f.delete();
      //throw new FileAlreadyExistsException(filename);
    }
    
    // create the file
    f.createNewFile();
    
    try {
      fileOut = new BufferedOutputStream(new FileOutputStream(filename));
    } catch (FileNotFoundException e) {
      // we just created the file so this error makes no sense
      e.printStackTrace();
    }
  }
  
  /**
   * Writes a block of data to the file
   * 
   * @param data
   * @return true on success / false on failure
   */
  public void writeBlock(byte[] data) throws IOException {    
    fileOut.write(data, 0, data.length);
  }
  
  /**
   * Closes the file output stream
   * 
   */
  public void close() {
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
