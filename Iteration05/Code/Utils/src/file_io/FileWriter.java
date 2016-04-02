package file_io;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;

public class FileWriter {
  private String filename;
  private FileLock fileLock = null;
  // use RandomAccessFile because it allows us to get an exclusive lock on the file
  private RandomAccessFile randomAccessFile = null;

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
    if (randomAccessFile == null) {
      this.open();
    }
    
    try {
      randomAccessFile.write(data);
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
		  randomAccessFile = new RandomAccessFile(filename, "rw");
		} catch (IOException e) {
      throw new AccessDeniedException("Insufficient permissions to write to file.");
    }
	
		// try to acquire an exclusive lock on the file. If it fails, someone else is reading/writing the file
		try {
			fileLock = randomAccessFile.getChannel().lock();
		} catch (IOException e) {
				throw new AccessDeniedException("Cannot acquire a lock on the file.");
		} catch (OverlappingFileLockException e) {
      throw new AccessDeniedException("File is in use. Please try again later.");
    }
  }
  
  /**
   * Closes the file output stream
   * 
   */
  public void close() {
    if (this.randomAccessFile == null) {
      return; 
    }
    
    try { 
    	this.fileLock.release();
    	this.randomAccessFile.close(); 
  	} catch (IOException e) {}
    
    this.randomAccessFile = null;
    this.fileLock = null;
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
    return "FileWriter [filename=" + filename + ", randomAccessFile=" + randomAccessFile + "]";
  }
}

