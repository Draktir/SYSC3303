package file_io;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AccessDeniedException;


public class FileReader {
  private String filename;
	private FileLock fileLock = null;
	// use RandomAccessFile because it allows us to get an exclusive lock on the file
	private RandomAccessFile randomAccessFile = null;
	
	public FileReader(String filename) throws FileNotFoundException, AccessDeniedException {
		File file = new File(filename);
		
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
      throw new AccessDeniedException("Insufficient persmissions to read file.");
    }
		
    this.filename = filename;
	}
	
  public byte[] read(final int blockSize) throws FileNotFoundException, AccessDeniedException {
  	if (randomAccessFile == null) {
  		open();
  	}
  	
  	byte[] buffer = new byte[blockSize];
  	int bytesRead = 0;
  	try {
			bytesRead = randomAccessFile.read(buffer);
		} catch (IOException e) {
			File f = new File(filename);
			
			if (!f.exists()) {
				throw new FileNotFoundException("File " + filename + " has been moved");
			}
			if (!f.canRead()) {
				throw new AccessDeniedException("Insufficient permissions to read file " + filename);
			}
			
			throw new AccessDeniedException("Cannot read from file " + filename);
		}
  	
  	bytesRead = bytesRead < 0 ? 0 : bytesRead;
  	
  	byte[] result = new byte[bytesRead];
  	System.arraycopy(buffer, 0, result, 0, bytesRead);
  	return result;
  }
  
	private void open() throws FileNotFoundException, AccessDeniedException {
		File file = new File(filename);
		
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
      throw new AccessDeniedException("Insufficient permissions to read file.");
    }
		
    // NOTE: Windows sucks! It will actually tell us that we ".canRead()", even though
    //       we do not have READ permissions for a file. So catch any error here and
    //       throw as an AccessDeniedException.
    try {
      randomAccessFile = new RandomAccessFile(filename, "rw");
    } catch (IOException e) {
      throw new AccessDeniedException("Insufficient permissions to read file.");
    }
    // try to acquire an exclusive lock. If it fails, the file is already in use.
  	try {
  	  fileLock = randomAccessFile.getChannel().lock();
  	} catch (IOException e) {
  	  throw new AccessDeniedException("Cannot acquire a lock on the file.");
  	} catch (OverlappingFileLockException e) {
  	  throw new AccessDeniedException("File is in use. Please try again later.");
  	}
	}
	
	public void close() {
		if (this.randomAccessFile != null) {
			try {
				fileLock.release();
				this.randomAccessFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			this.randomAccessFile = null;
			this.fileLock = null;
		}
	}
}
