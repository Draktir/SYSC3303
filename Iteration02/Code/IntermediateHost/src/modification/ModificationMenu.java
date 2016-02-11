package modification;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import intermediate_host.PacketModifier;

public class ModificationMenu {

  public PacketModifier show() {
    PacketModifier modifier = new PacketModifier();
    Scanner scan = new Scanner(System.in);
    int menuSelection = -1;
    
    
    // TODO: Error code 5!!!
    
    
    // main menu
    System.out.println("TFTP Error Simulator");
    System.out.println("\nWhich type of packet do you want to modify.");
    System.out.println("  [ 1 ] Read Request (RRQ)");
    System.out.println("  [ 2 ] Write Request (WRQ)");
    System.out.println("  [ 3 ] Data Packet (DATA)");
    System.out.println("  [ 4 ] Acknowledgement (ACK)");
    System.out.println("  [ 0 ] Make no modification");
    System.out.print(" > ");
    
    menuSelection = scan.nextInt();
    
    
    if(menuSelection == 1) {
    	// ReadRequest
        // we always modify the first read request
        ReadRequestModification readReqMode = new ReadRequestModification(); 
        int fieldSelection = -1;
        while (fieldSelection != 0) {
            System.out.println("Which field do you want to modify?");
            System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
            System.out.println("  [ 2 ] Filename");
            System.out.println("  [ 3 ] zero-byte after filename");
            System.out.println("  [ 4 ] Mode");
            System.out.println("  [ 5 ] zero-byte after mode");
            System.out.println("  [ 0 ] Done");
            System.out.println(" > ");
          
          fieldSelection = scan.nextInt();
          
          if (fieldSelection == 0){
            break;
          }
          
          System.out.println("How do you want to modify the field?");
          System.out.println("  [ 1 ] Replace with bytes");
          System.out.println("  [ 2 ] Replace with int");
          System.out.println("  [ 3 ] Replace with string");
          System.out.println("  [ 4 ] Remove field");
          System.out.print(" > ");
           
          int modType = scan.nextInt(); 
          byte[] modValue;
          
          Scanner modScanner = new Scanner(System.in);
          
          switch (modType) {
            case 1:
              System.out.print("Enter bytes separated by spaces: ");
              String bytesStr = modScanner.nextLine();
              
              String[] splitBytes = bytesStr.split(" ");
              modValue = new byte[splitBytes.length];
              
              for (int i = 0; i < splitBytes.length; i++) {
                BigInteger integer = new BigInteger(splitBytes[i]);
                modValue[i] = integer.byteValue();
              }
              break;
              
            case 2:
              System.out.print("Enter your int: ");
              BigInteger newInt = BigInteger.valueOf(scan.nextInt());
              modValue = newInt.toByteArray();
              if (modValue.length == 1) {
                modValue = new byte[] {0, modValue[0]};
              } else {
                modValue = new byte[] {modValue[0], modValue[1]};
              }
              break;
              
            case 3:
              System.out.print("Enter your string: ");
              String str = scan.nextLine();
              modValue = str.getBytes();
              break;
              
            case 4:
              modValue = new byte[0];
              break;
              
            default:
              System.err.println("Invalid selection");
              continue;
          }

          switch (fieldSelection) {
            case 1:
            	readReqMode.setOpcode(modValue);
              break;
            case 2:
            	readReqMode.setFilename(modValue);
              break;
            case 3:
//            	readReqMode.setZeroByteAfterFilename(modValue);TODO: wait for P change the function
            case 4:
            	readReqMode.setMode(modValue);
            case 5:
            	readReqMode.setMode(modValue);
//            	readReqMode.setZeroByteAfterMode(modValue);TODO: wait for P change the function            	
            default:
              System.err.println("Invalid field selection");
              break;
          }
          System.out.println(readReqMode);
          System.out.println("\n");
        }    	
    }
    
    
    /*
    
    // ReadRequest
    
    // we always modify the first read request
    
    System.out.println("Which field do you want to modify?");
    System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
    System.out.println("  [ 2 ] Filename");
    System.out.println("  [ 3 ] zero-byte after filename");
    System.out.println("  [ 3 ] Mode");
    System.out.println("  [ 4 ] zero-byte after mode");
    System.out.println(" > ");
    
    // WriteRequest
    
    // we always modify the first write request
 
    System.out.println("Which field do you want to modify?");
    System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
    System.out.println("  [ 2 ] Filename");
    System.out.println("  [ 3 ] zero-byte after filename");
    System.out.println("  [ 3 ] Mode");
    System.out.println("  [ 4 ] zero-byte after mode");
    System.out.println(" > ");
    
    
    // DataPacket
    System.out.print("Which one of the DataPackets do you want to modify? #");
    
    System.out.println("Which field do you want to modify?");
    System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
    System.out.println("  [ 2 ] Block Number (bytes 3 & 4)");
    System.out.println("  [ 3 ] Data");
    System.out.println(" > ");
    */
    
    if (menuSelection == 4) {
      int packetNumber;
      // Acknowledgement
      System.out.print("Which one of the Acknowledgements do you want to modify? #");
      packetNumber = scan.nextInt();
      
      AcknowledgementModification ackMod = new AcknowledgementModification(packetNumber);
      
      int fieldSelection = -1;
      while (fieldSelection != 0) {
        System.out.println("Enter your modifications");
        System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
        System.out.println("  [ 2 ] Block Number (bytes 3 & 4)");
        System.out.println("  [ 3 ] Append data to packet");
        System.out.println("  [ 0 ] Done");
        System.out.print(" > ");
        
        fieldSelection = scan.nextInt();
        
        if (fieldSelection == 0){
          break;
        }
        
        System.out.println("How do you want to modify the field?");
        System.out.println("  [ 1 ] Replace with bytes");
        System.out.println("  [ 2 ] Replace with int");
        System.out.println("  [ 3 ] Replace with string");
        System.out.println("  [ 4 ] Remove field");
        System.out.print(" > ");
         
        int modType = scan.nextInt(); 
        byte[] modValue;
        
        Scanner modScanner = new Scanner(System.in);
        
        switch (modType) {
          case 1:
            System.out.print("Enter bytes separated by spaces: ");
            String bytesStr = modScanner.nextLine();
            
            String[] splitBytes = bytesStr.split(" ");
            modValue = new byte[splitBytes.length];
            
            for (int i = 0; i < splitBytes.length; i++) {
              BigInteger integer = new BigInteger(splitBytes[i]);
              modValue[i] = integer.byteValue();
            }
            break;
            
          case 2:
            System.out.print("Enter your int: ");
            BigInteger newInt = BigInteger.valueOf(scan.nextInt());
            modValue = newInt.toByteArray();
            if (modValue.length == 1) {
              modValue = new byte[] {0, modValue[0]};
            } else {
              modValue = new byte[] {modValue[0], modValue[1]};
            }
            break;
            
          case 3:
            System.out.print("Enter your string: ");
            String str = scan.nextLine();
            modValue = str.getBytes();
            break;
            
          case 4:
            modValue = new byte[0];
            break;
            
          default:
            System.err.println("Invalid selection");
            continue;
        }

        switch (fieldSelection) {
          case 1:
            ackMod.setOpcode(modValue);
            break;
          case 2:
            ackMod.setBlockNumber(modValue);
            break;
          case 3:
            ackMod.setAppendToEnd(modValue);
          default:
            System.err.println("Invalid field selection");
            break;
        }
        System.out.println(ackMod);
        System.out.println("\n");
      }
      
      modifier.setAckModification(ackMod);      
    }
    
    return modifier;
  }
}
