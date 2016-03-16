package file_io;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;


public class FileReader {
	private BufferedInputStream fileIn;
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
  	if (fileIn == null) {
  		this.fileIn = open(this.filename);
  	}
  	
  	byte[] buffer = new byte[blockSize];
  	int bytesRead = 0;
  	try {
			bytesRead = fileIn.read(buffer);
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
  
	private BufferedInputStream open(String filename) throws FileNotFoundException, AccessDeniedException {
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
		
    return new BufferedInputStream(new FileInputStream(filename), 512);
	}
	
	public void close() {
		if (this.fileIn != null) {
			try {
				this.fileIn.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
