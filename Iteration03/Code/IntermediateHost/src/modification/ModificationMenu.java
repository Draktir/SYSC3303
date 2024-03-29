package modification;

import java.math.BigInteger;
import java.util.Scanner;

import intermediate_host.PacketModifier;

public class ModificationMenu {
  private Scanner scan = new Scanner(System.in);
  
  public PacketModifier show() {
    PacketModifier modifier = new PacketModifier();
    int menuSelection = -1;

    while (menuSelection < 0 || menuSelection > 5) {
      // main menu
      System.out.println("TFTP Error Simulator");
      System.out.println("\nWhich type of packet do you want to modify.");
      System.out.println("  [ 1 ] Read Request (RRQ)");
      System.out.println("  [ 2 ] Write Request (WRQ)");
      System.out.println("  [ 3 ] Data Packet (DATA)");
      System.out.println("  [ 4 ] Acknowledgement (ACK)");
      System.out.println("  [ 5 ] Error Packet (ERROR)");
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
    } else if (menuSelection == 5) {
      ErrorPacketModification errMod = configureErrorPacketModification();
      modifier.setErrorModification(errMod);
    } else {
      System.out.println("\nNo modifications will be made.");
    }
    
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
      System.out.println("  [ 7 ] Modify TID (sending port number)");
      System.out.println("  [ 8 ] Delay packet");
      System.out.println("  [ 9 ] Drop packet");
      System.out.println("  [ 10] Duplicate packet");
      System.out.println("  [ 0 ] Done");
      System.out.print(" > ");
    
      fieldSelection = scan.nextInt();
      
      if (fieldSelection <= 0 || fieldSelection > 10) {
        continue;
      }
      
      byte[] modValue = null;
      TidModification tidMod = null;
      DelayPacketModification delayMod = null;
      DropPacketModification dropMod = null;
      DuplicatePacketModification duplicateMod = null;
      
      if (fieldSelection == 7) {
        tidMod = configureTidModification();
      } else if (fieldSelection == 8) {
        delayMod = configureDelayPacketModification();
      } else if (fieldSelection == 9) {
        dropMod = new DropPacketModification();
      } else if (fieldSelection == 10) {
        duplicateMod = configureDuplicatePacketModification();
      } else {
        modValue = getModValueFromUser();
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
          break;
        case 7:
          readReqMod.setTidModification(tidMod);
          break;
        case 8:
          readReqMod.setDelayModification(delayMod);
          break;
        case 9:
          readReqMod.setDropModification(dropMod);
          System.out.println("Packet will be drop ped");
          break;
        case 10:
          readReqMod.setDuplicatePacketModification(duplicateMod);
          break;
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
      System.out.println("\nWhich field do you want to modify?");
      System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
      System.out.println("  [ 2 ] Filename");
      System.out.println("  [ 3 ] zero-byte after filename");
      System.out.println("  [ 4 ] Mode");
      System.out.println("  [ 5 ] zero-byte after mode");
      System.out.println("  [ 6 ] Append data at the end");
      System.out.println("  [ 7 ] Modify TID (sending port number)");
      System.out.println("  [ 8 ] Delay packet");
      System.out.println("  [ 9 ] Drop packet");
      System.out.println("  [ 10] Duplicate packet");
      System.out.println("  [ 0 ] Done");
      System.out.print(" > ");
    
      fieldSelection = scan.nextInt();
      
      if (fieldSelection <= 0 || fieldSelection > 10) {
        continue;
      }
      
      byte[] modValue = null;
      TidModification tidMod = null;
      DelayPacketModification delayMod = null;
      DropPacketModification dropMod = null;
      DuplicatePacketModification duplicateMod = null;
      
      if (fieldSelection == 7) {
        tidMod = configureTidModification();
      } else if (fieldSelection == 8) {
        delayMod = configureDelayPacketModification();
      } else if (fieldSelection == 9) {
        dropMod = new DropPacketModification();
      } else if (fieldSelection == 10) {
        duplicateMod = configureDuplicatePacketModification();
      } else {
        modValue = getModValueFromUser();
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
        case 7:
          writeReqMod.setTidModification(tidMod);
          break;
        case 8:
          writeReqMod.setDelayModification(delayMod);
          break;
        case 9:
          writeReqMod.setDropModification(dropMod);
          System.out.println("Packet will be dropped");
          break;
        case 10:
          writeReqMod.setDuplicatePacketModification(duplicateMod);
          break;
        default:
          System.err.println("Invalid field selection");
          continue;
      }
    }
   
    System.out.println("\n" + writeReqMod.toString());
    System.out.println("\n");
    
    return writeReqMod;
  }
  
  private DataPacketModification configureDataPacketModification() {
    int packetNumber;
    System.out.print("\nWhich one of the Data Packets do you want to modify? #");
    packetNumber = scan.nextInt();
    
    DataPacketModification dataMod = new DataPacketModification(packetNumber);
    
    int fieldSelection = -1;
    while (fieldSelection != 0) {
      System.out.println("\nWhich field do you want to modify?");
      System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
      System.out.println("  [ 2 ] Block Number (bytes 3 & 4)");
      System.out.println("  [ 3 ] Data");
      System.out.println("  [ 4 ] Append to data field");
      System.out.println("  [ 5 ] Modify TID (sending port number)");
      System.out.println("  [ 6 ] Delay packet");
      System.out.println("  [ 7 ] Drop packet");
      System.out.println("  [ 8 ] Duplicate packet");
      System.out.println("  [ 0 ] Done");
      System.out.print(" > ");
      
      fieldSelection = scan.nextInt();
      
      if (fieldSelection <= 0 || fieldSelection > 8) {
        continue;
      }
      
      byte[] modValue = null;
      TidModification tidMod = null;
      DelayPacketModification delayMod = null;
      DropPacketModification dropMod = null;
      DuplicatePacketModification duplicateMod = null;
      
      if (fieldSelection == 5) {
        tidMod = configureTidModification();
      } else if (fieldSelection == 6) {
        delayMod = configureDelayPacketModification();
      } else if (fieldSelection == 7) {
        dropMod = new DropPacketModification();
      } else if (fieldSelection == 8) {
        duplicateMod = configureDuplicatePacketModification();
      } else {
        modValue = getModValueFromUser();
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
          break;
        case 5:
          dataMod.setTidModification(tidMod);
          break;
        case 6:
          dataMod.setDelayModification(delayMod);
          break;
        case 7:
          dataMod.setDropModification(dropMod);
          System.out.println("Packet will be dropped");
          break;
        case 8:
          dataMod.setDuplicatePacketModification(duplicateMod);
          break;
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
    System.out.print("\nWhich one of the Acknowledgements do you want to modify? #");
    packetNumber = scan.nextInt();
    
    AcknowledgementModification ackMod = new AcknowledgementModification(packetNumber);
    
    int fieldSelection = -1;
    while (fieldSelection != 0) {
      System.out.println("\nWhich field do you want to modify?");
      System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
      System.out.println("  [ 2 ] Block Number (bytes 3 & 4)");
      System.out.println("  [ 3 ] Append data at the end");
      System.out.println("  [ 4 ] Modify TID (sending port number)");
      System.out.println("  [ 5 ] Delay packet");
      System.out.println("  [ 6 ] Drop packet");
      System.out.println("  [ 7 ] Duplicate packet");
      System.out.println("  [ 0 ] Done");
      System.out.print(" > ");
      
      fieldSelection = scan.nextInt();
      
      if (fieldSelection <= 0 || fieldSelection > 7) {
        continue;
      }
      
      byte[] modValue = null;
      TidModification tidMod = null;
      DelayPacketModification delayMod = null;
      DropPacketModification dropMod = null;
      DuplicatePacketModification duplicateMod = null;
      
      if (fieldSelection == 4) {
        tidMod = configureTidModification();
      } else if (fieldSelection == 5) {
        delayMod = configureDelayPacketModification();
      } else if (fieldSelection == 6) {
        dropMod = new DropPacketModification();
      } else if (fieldSelection == 7) {
        duplicateMod = configureDuplicatePacketModification();
      } else {
        modValue = getModValueFromUser();
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
        case 4:
          ackMod.setTidModification(tidMod);
          break;
        case 5:
          ackMod.setDelayModification(delayMod);
          break;
        case 6:
          ackMod.setDropModification(dropMod);
          System.out.println("Packet will be dropped");
          break;
        case 7:
          ackMod.setDuplicatePacketModification(duplicateMod);
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
  
  private ErrorPacketModification configureErrorPacketModification() {
    int packetNumber;
    System.out.print("\nWhich one of the Error Packets do you want to modify? #");
    packetNumber = scan.nextInt();
    
    ErrorPacketModification errorMod = new ErrorPacketModification(packetNumber);
    
    int fieldSelection = -1;
    while (fieldSelection != 0) {
      System.out.println("\nWhich field do you want to modify?");
      System.out.println("  [ 1 ] Opcode (bytes 1 & 2)");
      System.out.println("  [ 2 ] Error Code (bytes 3 & 4)");
      System.out.println("  [ 3 ] Error Message");
      System.out.println("  [ 4 ] Zero Byte after Message");
      System.out.println("  [ 5 ] Modify TID (sending port number)");
      System.out.println("  [ 6 ] Delay packet");
      System.out.println("  [ 7 ] Drop packet");
      System.out.println("  [ 8 ] Duplicate packet");
      System.out.println("  [ 0 ] Done");
      System.out.print(" > ");
      
      fieldSelection = scan.nextInt();
      
      if (fieldSelection <= 0 || fieldSelection > 8) {
        continue;
      }

      byte[] modValue = null;
      TidModification tidMod = null;
      DelayPacketModification delayMod = null;
      DropPacketModification dropMod = null;
      DuplicatePacketModification duplicateMod = null;
      
      if (fieldSelection == 5) {
        tidMod = configureTidModification();
      } else if (fieldSelection == 6) {
        delayMod = configureDelayPacketModification();
      } else if (fieldSelection == 7) {
        dropMod = new DropPacketModification();
      } else if (fieldSelection == 7) {
        duplicateMod = configureDuplicatePacketModification();
      } else {
        modValue = getModValueFromUser();
      }

      switch (fieldSelection) {
        case 1:
          errorMod.setOpcode(modValue);
          break;
        case 2:
          errorMod.setErrorCode(modValue);
          break;
        case 3:
          errorMod.setErrorMessge(modValue);
          break;
        case 4:
          errorMod.setZeroByteAfterMessage(modValue);
          break;
        case 5:
          errorMod.setTidModification(tidMod);
          break;
        case 6:
          errorMod.setDelayModification(delayMod);
          break;
        case 7:
          errorMod.setDropModification(dropMod);
          System.out.println("Packet will be dropped");
          break;
        case 8:
          errorMod.setDuplicatePacketModification(duplicateMod);
          break;
        default:
          System.err.println("Invalid field selection");
          break;
      }
    }
    
    System.out.println("\n" + errorMod.toString());
    System.out.println("\n");
    
    return errorMod;
  }
  
  private TidModification configureTidModification() {
    int port = 0;
    while (port <= 0) {
      System.out.print("\nEnter the new sending port: ");
      port = scan.nextInt();
    }
    return new TidModification(port);
  }
  
  private DelayPacketModification configureDelayPacketModification() {
    int delay = -1;
    while (delay < 0) {
      System.out.println("\nEnter the delay in milliseconds: ");
      delay = scan.nextInt();
    }
    return new DelayPacketModification(delay);
  }
  
  private DuplicatePacketModification configureDuplicatePacketModification() {
    int delay = -1;
    int duplications = -1;
    
    while (delay < 0) {
      System.out.print("\nEnter the delay between sending duplicate(s) in milliseconds: ");
      delay = scan.nextInt();
    }
    
    while (duplications < 0) {
      System.out.print("\nEnter the number of duplications: ");
      duplications = scan.nextInt();
    }
    
    return new DuplicatePacketModification(delay, duplications);
  }
  
  private byte[] getModValueFromUser() {
    int modType = -1;
    byte[] modValue = null;
    
    while (modType < 1 || modType > 4) {
      System.out.println("\nHow do you want to modify the field?");
      System.out.println("  [ 1 ] Replace with bytes");
      System.out.println("  [ 2 ] Replace with int");
      System.out.println("  [ 3 ] Replace with string");
      System.out.println("  [ 4 ] Remove field");
      System.out.print(" > ");
       
      modType = scan.nextInt(); 
      
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
      }
    }
    return modValue;
  }
  
  private byte[] getByteUserInput() {
    System.out.print("Enter bytes separated by spaces:");
    scan.nextLine(); // don't ask me why we need this, Java's Scanner is a pain in the ...
    String bytesStr = scan.nextLine(); 

    String[] splitBytes = bytesStr.split(" ");
    byte[] result = new byte[splitBytes.length];

    for (int i = 0; i < splitBytes.length; i++) {
      BigInteger integer = new BigInteger(splitBytes[i]);
      result[i] = integer.byteValue();
    }

    return result;
  }

  private byte[] getIntUserInput() {
    System.out.print("Enter an int: ");
    BigInteger newInt = BigInteger.valueOf(scan.nextInt());
    byte[] result = newInt.toByteArray();

    // pad with a zero if length < 2
    if (result.length == 1) {
      result = new byte[] { 0, result[0] };
    }

    return result;
  }

  private byte[] getStringUserInput() {
    System.out.print("Enter a string: ");
    scan.nextLine();
    String str = scan.nextLine();

    return str.getBytes();
  }
}

