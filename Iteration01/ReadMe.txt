=================================
           Group #4
         
         Iteration #1
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
      packet/
        *.java - Java classes encapsulating the TFTP packets sent over the network
   IntermediateHost/
     src/
       intermediate_host/
         *.java - Java source code for the Intermediate Host / Error Simulator
   Server/
      testReadFile.txt - file that's downloaded from the server for testing
      src/
        server/
          *.java - Java source code for the TFTP Server
        packet/
          *.java - Java classes encapsulating the TFTP packets sent over the network

Diagrams/
  Client_ClassDiagram.png - class diagram for client
  Packets_ClassDagram.png - class diagram for TFTP Packet classes
  WRQ.JPG - UCM diagram for Write Request
  IntermediateHost_ClassDiagram.png - class diagram for Intermediate Host
  RRQ.JPG - UCM diagram for Read Request
  PacketBuilder_ClassDiagram.png - class diagram for Packet Builder helper classes
  Server_ClassDiagram.png - class diagram for server




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
  3) Right click on the "Client" Project and select "Run As" -> "Java Application"

Testing
  1) In the client application type "1" and hit enter.
  2) The client will transfer the testWriteFile.txt to the Server the file should appear in the server
     directory (Code/Server/testWriteFile.txt) once the transfer is complete.
  3) Stop and restart the intermediate host
  4) In the client applicatio type "2" and hit enter.
  5) The client will download the testReadFile.txt from the Server the file should appear in the client
     directory (Code/Client/testReadFile.txt) once the transfer is complete,

Shutdown
  1) To terminate the server type "shutdown" in the server application window and hit enter.

N.B: The server can remain running for the entire time and is capable of handling multiple requests.
     The client can also transfer the files multiple times without requiring a restart.
     Only the intermediate host requires a restart after a transfer, since it is only used for testing.
     To connect directly from the client to the server change the SERVER_PORT constant in the client to 
     69.
