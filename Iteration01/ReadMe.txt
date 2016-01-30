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





Read Me:
- Start the Server first then the IntermediatHost then the Client.
- The Client will ask fot the file name which can be as big as 100 charecters
- send requests using the TCTP protocol descriped bellow.

- the client will either send a RRQ or a WRQ using the the TCTP protocol.
  for the requests:
	RRQ = 01"block number" 0 "mode" 0.
	WRQ = 02"Data acquired" 0 "mode" 0.
	any other requests will be invalid and will case the server to close.

- the server will be reading and writing from an earlier created file that will be alocated in the same folder of the project.

- the server can receive as much requests as possible as it will be using multithreading which will allow it to handle many requests at a time.
 
