=================================
           Group #4
         
         Iteration #3
=================================


Files:
----------------------
ReadMe.txt - this file
Code/
  Client/
    testWriteFile.txt - file that's uploaded to the server for testing
    src/
      client/
        *.java - Java source code for the TFTP client
  Configuration/
    src/Configuration/
      Configuration.java - Java class containing port numbers
  FileIO/
    src/file_io
      *.java - Java classes for file reading and writing
  IntermediateHost/
     src/
       intermediate_host/
         *.java - Java source code for the Intermediate Host / Error Simulator
       modification/
         *.java - Java source code for modification of packets and error coniguration menu
   Packet/
     src/   
       *.java - Java classess encapsulating TFTP packets, parsing and building of TFTP packets
   Server/
      testReadFile.txt - file that's downloaded from the server for testing
      src/
        server/
          *.java - Java source code for the TFTP Server

Diagrams/
  RRQ.png - UCM diagram for a ReadRequest
  WRQ.png - UCM diagram for a WriteRequest
  ClassDiagrams/
    *.png - class diagrams for the system
  TimingDiagrams/
    *.png - timing diagrams for error scenarios


Launching the application
--------------------------

Import the projects
  1) Open eclipse
  2) Select File -> Import...
  3) Select General -> "Existing Projects into Workspace"
  4) Next to "Select root directory" click "Browse"
  5) Select the Code folder of this submission
  6) Click "Finish"

Run the program
  1) Right click on the "Server" Project and select "Run As" -> "Java Application"
  2) Right click on the "IntermediateHost" Project and select "Run As" -> "Java Application"
    2.1) configure the desired error scenario (see below)
  3) Right click on the "Client" Project and select "Run As" -> "Java Application"


Configure the Intermediate Host
-------------------------------
The intermediate host has an extensive menu that allows for modifications.
To delay / duplicate / drop a packet, first select the packet type and then the number
of the packet you want to modify (RRQ and WRQ it's always the first packet).
Then select one of the options (e.g. delay packet)
There are a number of test files in both the Server and Client folder.
These test files are named according to their size.

