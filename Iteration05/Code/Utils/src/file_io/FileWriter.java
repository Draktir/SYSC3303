package file_io;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;

public class FileWriter {
  private String filename;
  private FileLock fileLock = null;
  private FileOutputStream fileOut = null;
  private BufferedOutputStream bufferedOut = null;

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
      throw new AccessDeniedException("Insufficient permissions to create " + filename);
    }
    
    // this is highly unlikely since we were able to create the file,
    // but doesn't hurt to check.
    if (!file.exists() || !file.canWrite()) {
      file.delete();
      throw new AccessDeniedException("Insufficient permissions to write to " + filename);
    }
  }
  
  /**
   * writes a block of data to the file
   * @param data
   * @throws FileAlreadyExistsException
   * @throws AccessDeniedException
   * @throws FileNotFoundException
   * @throws DiskFullException
   */
  public void writeBlock(byte[] data) throws 
  		AccessDeniedException, FileNotFoundException, DiskFullException, FileAlreadyExistsException {    
    if (bufferedOut == null) {
      this.open();
    }
    
    try {
    	bufferedOut.write(data, 0, data.length);
    	bufferedOut.flush();
    } catch (IOException e) {
    	File f = new File(filename);

    	if (!f.exists()) {
    		throw new FileNotFoundException("The file has been moved");
    	} else if (!f.canWrite()) {
    		throw new AccessDeniedException("Insufficient permission to write to file.");
    	} else {
    		throw new DiskFullException("The disk is full");
    	}
    }
  }
  
  private void open() throws FileAlreadyExistsException, AccessDeniedException {
  	File f = new File(filename);
    if (!f.exists()) {
    	createFile();
    }
    // NOTE: Windows lies and tells us we ".canWrite()" a file even if we do not have
    //       the right permissions. So we interpret this as an AccessDeniedException.
		try {
			fileOut = new FileOutputStream(filename);
			
			// try to acquire an exclusive lock on the file. If it fails, someone else is reading/writing the file
			try {
				fileLock = fileOut.getChannel().lock();
			} catch (IOException e) {
				throw new AccessDeniedException("The file is currently being used. Try again later.");
			}
			
			bufferedOut = new BufferedOutputStream(fileOut);
		} catch (IOException e) {
			throw new AccessDeniedException("Insufficient permissions to write to file.");
		}
  }
  
  /**
   * Closes the file output stream
   * 
   */
  public void close() {
    if (this.bufferedOut == null) {
      return; 
    }
    
    try { 
    	this.fileLock.release();
    	this.bufferedOut.close(); 
  	} catch (IOException e) {}
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
    return "FileWriter [filename=" + filename + ", fileOut=" + bufferedOut + "]";
  }
}

