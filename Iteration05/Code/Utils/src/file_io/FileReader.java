package file_io;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.AccessDeniedException;


public class FileReader {
	private FileLock fileLock;
	private RandomAccessFile randomAccessFile;
	private BufferedInputStream bufferedIn;
	private String filename;
	
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
  	if (bufferedIn == null) {
  		open(this.filename);
  	}
  	
  	byte[] buffer = new byte[blockSize];
  	int bytesRead = 0;
  	try {
			bytesRead = bufferedIn.read(buffer);
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
  
	private void open(String filename) throws FileNotFoundException, AccessDeniedException {
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
    	
    	// try to acquire an exclusive lock. If it fails, the file is already in use.
    	try {
    		fileLock = randomAccessFile.getChannel().lock();
    	} catch (IOException e) {
    		throw new AccessDeniedException("File is in use. Please try again later.");
    	}
    		
    	bufferedIn = new BufferedInputStream(new FileInputStream(filename), 512);
    } catch (IOException e) {
    	throw new AccessDeniedException("Insufficient permissions to read file.");
    }
	}
	
	public void close() {
		if (this.bufferedIn != null) {
			try {
				fileLock.release();
				this.randomAccessFile.close();
				this.bufferedIn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
