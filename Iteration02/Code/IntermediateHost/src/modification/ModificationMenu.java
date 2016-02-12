package modification;

import java.math.BigInteger;
import java.util.Scanner;

import intermediate_host.PacketModifier;

public class ModificationMenu {
  private Scanner scan = new Scanner(System.in);
  
  public PacketModifier show() {
    PacketModifier modifier = new PacketModifier();
    int menuSelection = -1;

    while (menuSelection <= 0 || menuSelection > 4) {
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
    }

    if (menuSelection == 1) {
      ReadRequestModification rrqMod = configureReadRequestModification();
      modifier.setRrqModification(rrqMod);
    } else if (menuSelection == 2) {
      WriteRequestModification wrqMod = configureWriteRequestModification();
      modifier.setWrqModification(wrqMod);
    } else if (menuSelection == 3) {
      DataPacketModification dataMod = configureDataPacketModification();
      modifier.setDataModification(dataMod);
    } else if (menuSelection == 4) {
      AcknowledgementModification ackMod = configureAcknowledgementModification();
      modifier.setAckModification(ackMod);
    } else {
      System.err.println("You have made an invalid selection! No modifications will be made.");
    }
    
    scan.close();
    return modifier;
  }

  private ReadRequestModification configureReadRequestModification() {
    // we always modify the first read request
    ReadRequestModification readReqMod = new ReadRequestModification(); 
    int fieldSelection = -1;
    
    while (fieldSelection != 0) {
      System.out.println("\nWhich field do you want to modify?");
      System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
      System.out.println("  [ 2 ] Filename");
      System.out.println("  [ 3 ] zero-byte after filename");
      System.out.println("  [ 4 ] Mode");
      System.out.println("  [ 5 ] zero-byte after mode");
      System.out.println("  [ 6 ] Append data at the end");
      System.out.println("  [ 0 ] Done");
      System.out.print(" > ");
    
      fieldSelection = scan.nextInt();
      
      if (fieldSelection <= 0 || fieldSelection > 6) {
        continue;
      }
      
      System.out.println("\nHow do you want to modify the field?");
      System.out.println("  [ 1 ] Replace with bytes");
      System.out.println("  [ 2 ] Replace with int");
      System.out.println("  [ 3 ] Replace with string");
      System.out.println("  [ 4 ] Remove field");
      System.out.print(" > ");
       
      int modType = scan.nextInt(); 
      byte[] modValue;
      
      switch (modType) {
        case 1:
          modValue = getByteUserInput();
          break;
        case 2:
          modValue = getIntUserInput();
          break;
        case 3:
          modValue = getStringUserInput();
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
          readReqMod.setOpcode(modValue);
          break;
        case 2:
          readReqMod.setFilename(modValue);
          break;
        case 3:
          readReqMod.setZeroByteAfterFilename(modValue);
          break;
        case 4:
          readReqMod.setMode(modValue);
          break;
        case 5:
          readReqMod.setZeroByteAfterMode(modValue);
          break;
        case 6:
          readReqMod.setAppendToEnd(modValue);
        default:
          System.err.println("Invalid field selection");
          continue;
      }
    }
    
    System.out.println("\n" + readReqMod.toString());
    System.out.println("\n");
    
    return readReqMod;
  }

  private WriteRequestModification configureWriteRequestModification() {
    // we always modify the first write request
    WriteRequestModification writeReqMod = new WriteRequestModification(); 
    int fieldSelection = -1;
    
    while (fieldSelection != 0) {
      System.out.println("Which field do you want to modify?");
      System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
      System.out.println("  [ 2 ] Filename");
      System.out.println("  [ 3 ] zero-byte after filename");
      System.out.println("  [ 4 ] Mode");
      System.out.println("  [ 5 ] zero-byte after mode");
      System.out.println("  [ 6 ] Append data at the end");
      System.out.println("  [ 0 ] Done");
      System.out.print(" > ");
    
      fieldSelection = scan.nextInt();
      
      if (fieldSelection <= 0 || fieldSelection > 6) {
        continue;
      }
      
      System.out.println("How do you want to modify the field?");
      System.out.println("  [ 1 ] Replace with bytes");
      System.out.println("  [ 2 ] Replace with int");
      System.out.println("  [ 3 ] Replace with string");
      System.out.println("  [ 4 ] Remove field");
      System.out.print(" > ");
       
      int modType = scan.nextInt(); 
      byte[] modValue;
      
      switch (modType) {
        case 1:
          modValue = getByteUserInput();
          break;
        case 2:
          modValue = getIntUserInput();
          break;
        case 3:
          modValue = getStringUserInput();
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
          writeReqMod.setOpcode(modValue);
          break;
        case 2:
          writeReqMod.setFilename(modValue);
          break;
        case 3:
          writeReqMod.setZeroByteAfterFilename(modValue);
          break;
        case 4:
          writeReqMod.setMode(modValue);
          break;
        case 5:
          writeReqMod.setZeroByteAfterMode(modValue);
          break;
        case 6:
          writeReqMod.setAppendToEnd(modValue);
          break;
        default:
          System.err.println("Invalid field selection");
          continue;
      }
      
      System.out.println("\n" + writeReqMod.toString());
      System.out.println("\n");
    }
   
    return writeReqMod;
  }
  
  private DataPacketModification configureDataPacketModification() {
    int packetNumber;
    System.out.print("Which one of the Data Packets do you want to modify? #");
    packetNumber = scan.nextInt();
    
    DataPacketModification dataMod = new DataPacketModification(packetNumber);
    
    int fieldSelection = -1;
    while (fieldSelection != 0) {
      System.out.println("Which field do you want to modify?");
      System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
      System.out.println("  [ 2 ] Block Number (bytes 3 & 4)");
      System.out.println("  [ 3 ] Data");
      System.out.println("  [ 4 ] Append data at the end");
      System.out.println("  [ 0 ] Done");
      System.out.print(" > ");
      
      fieldSelection = scan.nextInt();
      
      if (fieldSelection <= 0 || fieldSelection > 4) {
        continue;
      }
      
      System.out.println("How do you want to modify the field?");
      System.out.println("  [ 1 ] Replace with bytes");
      System.out.println("  [ 2 ] Replace with int");
      System.out.println("  [ 3 ] Replace with string");
      System.out.println("  [ 4 ] Remove field");
      System.out.print(" > ");
       
      int modType = scan.nextInt(); 
      byte[] modValue;

      switch (modType) {
        case 1:
          modValue = getByteUserInput();
          break;
        case 2:
          modValue = getIntUserInput();
          break;
        case 3:
          modValue = getStringUserInput();
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
          dataMod.setOpcode(modValue);
          break;
        case 2:
          dataMod.setBlockNumber(modValue);
          break;
        case 3:
          dataMod.setData(modValue);
          break;
        case 4:
          dataMod.setAppendToEnd(modValue);
        default:
          System.err.println("Invalid field selection");
          break;
      }
    }
    
    System.out.println("\n" + dataMod.toString());
    System.out.println("\n");
    
    return dataMod;
  }

  private AcknowledgementModification configureAcknowledgementModification() {
    int packetNumber;
    System.out.print("Which one of the Acknowledgements do you want to modify? #");
    packetNumber = scan.nextInt();
    
    AcknowledgementModification ackMod = new AcknowledgementModification(packetNumber);
    
    int fieldSelection = -1;
    while (fieldSelection != 0) {
      System.out.println("Which field do you want to modify?");
      System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
      System.out.println("  [ 2 ] Block Number (bytes 3 & 4)");
      System.out.println("  [ 3 ] Append data at the end");
      System.out.println("  [ 0 ] Done");
      System.out.print(" > ");
      
      fieldSelection = scan.nextInt();
      
      if (fieldSelection <= 0 || fieldSelection > 3) {
        continue;
      }
      
      System.out.println("How do you want to modify the field?");
      System.out.println("  [ 1 ] Replace with bytes");
      System.out.println("  [ 2 ] Replace with int");
      System.out.println("  [ 3 ] Replace with string");
      System.out.println("  [ 4 ] Remove field");
      System.out.print(" > ");
       
      int modType = scan.nextInt(); 
      byte[] modValue;

      switch (modType) {
        case 1:
          modValue = getByteUserInput();
          break;
        case 2:
          modValue = getIntUserInput();
          break;
        case 3:
          modValue = getStringUserInput();
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
          break;
        default:
          System.err.println("Invalid field selection");
          break;
      }
    }
    
    System.out.println("\n" + ackMod.toString());
    System.out.println("\n");
    
    return ackMod;
  }
  
  private byte[] getByteUserInput() {
    System.out.print("Enter bytes separated by spaces: ");
    String bytesStr = scan.next();

    String[] splitBytes = bytesStr.split(" ");
    byte[] result = new byte[splitBytes.length];

    for (int i = 0; i < splitBytes.length; i++) {
      BigInteger integer = new BigInteger(splitBytes[i]);
      result[i] = integer.byteValue();
    }

    return result;
  }

  private byte[] getIntUserInput() {
    System.out.print("Enter your int: ");
    BigInteger newInt = BigInteger.valueOf(scan.nextInt());
    byte[] result = newInt.toByteArray();

    // pad with a zero if length < 2
    if (result.length == 1) {
      result = new byte[] { 0, result[0] };
    }

    return result;
  }

  private byte[] getStringUserInput() {
    System.out.print("Enter your string: ");
    String str = scan.next();

    return str.getBytes();
  }
}

