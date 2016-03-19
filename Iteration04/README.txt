=================================
           Group #4
         
         Iteration #4
=================================

==============================================
Files:
==============================================
ReadMe.txt - this file

Code/
  Client/           - Code for Client
  IntermediateHost/ - Code for IntermediateHost
  Server/           - Code for Server
  Utils/            - Common code between Server,Client,IntermediateHost

Diagrams/
  ClassDiagrams/                - Class Diagrams for the system
  errors_4_5_TimingDiagrams/    - Error codes 4 & 5 timing diagrams (iteration 2) 
  lost_delayed_TimingDiagrams/  - Lost/Delayed packet timing diagrams (iteration 3)
  UCMs/                         - Use Case maps (iteration 1)
  fileIO_TimingDiagrams/        - Error codes 1, 2, 3, 6 (iteration 4)
    accessViolation_RRQ.png             - Access Violation during RRQ
    accessViolation_WRQ.png             - Access Violation during WRQ
    diskFullOrAllocationExceed_RRQ.png  - Disk Full during RRQ
    diskFullOrAllocationExceed_WRQ.png  - Disk Full during WRQ
    fileAlreadyExist_WRQ.png            - File Already exists on WRQ
    fileNotFound_RRQ.png                - File not found beginning of RRQ
    fileNotFound_RRQ2.png               - File not found during RRQ (file was moved)
    fileNotFound_WRQ.png                - File not found during WRQ (file was moved)


==============================================
Launching the application
==============================================

Import the projects
  1) Open eclipse
  2) Select File -> Import...
  3) Select General -> "Existing Projects into Workspace"
  4) Next to "Select root directory" click "Browse"
  5) Select the Code folder of this submission
  6) Click "Finish"

Run the program
  1) Right click on the "Server" Project and select "Run As" -> "Java Application"
    1.1) Select a configuration mode (usually Debug Mode)
  2) Right click on the "IntermediateHost" Project and select "Run As" -> "Java Application"
    2.1) Select a configuration mode (usually Debug Mode)
    2.2) configure the desired error scenario (see below) or "No Modification"
  3) Right click on the "Client" Project and select "Run As" -> "Java Application"
    3.1) Select a configuration mode (usually Debug Mode)


==========================================================
Select a Configuration for Client/Server/Intermediate
==========================================================
Upon start the Client/Server/IntermediateHost will ask which configuration mode Vto use. The mode
should always be the same between all of them, except when using Manual mode, which allows to modify
everything for a particular program.

Debug Mode: Verbose output, use IntermediateHost
  - Verbose logging output
  - 5000ms socket timeout
  - Make 3 attempts to send a packet in case of timeout
  - Client connects to port 68 (intermediate port)
  - Intermediate listens on port 68
  - Server listens on port 69
  - File block size is 512 bytes
  - File path is empty => use default directory (see below)

Test Mode: Verbose output, ignore IntermediateHost
  - Verbose logging output
  - 5000ms socket timeout
  - Make 3 attempts to send a packet in case of timeout
  - Client connects to port 69 (server port)
  - Intermediate listens on port 68
  - Server listens on port 69
  - File block size is 512 bytes
  - File path is empty => use default directory (see below)


Quiet Mode: very little logging output, ignore IntermediateHost
  - Very little logging
  - 5000ms socket timeout
  - Make 3 attempts to send a packet in case of timeout
  - Client connects to port 69 (server port)
  - Intermediate listens on port 68
  - Server listens on port 69
  - File block size is 512 bytes
  - File path is empty => use default directory (see below)

Linux Mode (for testing): verbose output, uses different ports to avoid permission problems
  - verbose logging output
  - 5000ms socket timeout
  - Make 3 attempts to send a packet in case of timeout
  - Client connects to port 6900 (server port)
  - Intermediate listens on port 6800
  - Server listens on port 6900
  - File block size is 512 bytes
  - File path is empty => use default directory (see below)


Manual Mode: Everything can be configured by the user

==============================================
Configure the Intermediate Host
==============================================
After selecting a configuration the intermediate host displays an extensive menu that allows for modifications.
To delay / duplicate / drop a packet, first select the packet type and then the number
of the packet you want to modify (RRQ and WRQ is always the first packet).
Then select one of the options (e.g. delay packet).
There are a number of test files in both the Server and Client folder.
These test files are named according to their size.


==============================================
Transfer File locations
==============================================
- By default, files on the server are placed in the Code/Server directory
- By default, files on the client are placed in the Code/Client directory
- It is possible to modify the locations by selecting Manual mode when asked
  for a configuration mode when the program first starts.



==============================================
ERROR SCENARIOS
==============================================

NOTE:
The Client and Server contain a number of test files in their respective default directories.
These files are named after their size (e.g. two-blocks will be slightly smaller than 1024 bytes,
two-blocks-eactly will be eactly 1024 bytes big).
These files are identical on both the client and server, so you may need to delete them on either side
to test successful transmissions.


File Not Found:
---------------------
There are two possible scenarios in which this error may occur:

1) The file does not exist on the server:

   The server received a RRQ but the filename contained in the request cannot be found.
   In that case the server sends an error (Code 1) and aborts the transfer.

   On the client on a Write Request if the user enters a filename and that file does not exist, 
   the client will show an error message and NOT send a Write Request, therefore there is no
   Error being sent either.

2) The file goes away during transfer (e.g. USB key unplugged)
   
   On the server, if during a Read Transfer, the file we are reading from is (re)moved,
   the server cannot continue to read from it and thus will send an Error Code 1, and 
   abort the transfer.
   On the client, if during a Write Transfer, the file we are reading from is (re)moved 
   the Client sends an Error Code 1, and aborts the transfer.

   Note that we are using a Buffered Input Stream, so for small files this will not
   occur, since the entire file will already be in the buffer.


Testing:
  - Client:
    - Try to send a file that does not exist in the configured path.
    - Configure the client (Manual Mode) to use a path to a USB key
      Then initiate a file transfer and unplug the USB key mid-transfer.
  - Server:
    - Try to send a RRQ for a file that does not exist on the server.
    - Configure the client (Manual Mode) to use a path to a USB key
      Then initiate a file transfer and unplug the USB key mid-transfer.



Access Violation:
--------------------
On the Client:
   If, on a Write Transfer, the file the user selects cannot be read, the user is informed
   with an error message and we DO NOT send a WRQ to the server. Therefore the is no error packet.
   On a Read Transfer the Client will first attempt to create a file. Only if that's successful
   will it send the RRQ.

On the Server:
   If the server receives a RRQ or WRQ, and  trying to read or write the file is not possible 
   because the permissions are insufficient (no read/write access), the server responds with an 
   Error code 2 and terminates the transfer.

Note that if the permissions change during a transfer, it does not affect the program, since it 
already opened the file and has a valid file descriptor. Thus it can continue to read/write and
complete the transfer.

Testing:
  - Client:
    - Configure the client (Manual Mode) to use a path to C:\Users\[someOtherUser]
      Then do a RRQ or WRQ.
  - Server:
    - Configure the server (Manual Mode) to use a path to C:\Users\[someOtherUser]
      Then do a RRQ or WRQ from the client.


Disk Full:
---------------------
If trying to write a block to a file on disk fails, because the disk is full, both client
and server will respond in the same way. First an error message (Code 3) is sent and then
the file that had been written to is deleted, since it is incomplete.

Testing:
  - Client:
    - Configure the client (Manual Mode) to use a path to a USB key that's full
      Then do a RRQ
  - Server:
    - Configure the server (Manual Mode) to use a path to s USB key that's full
      Then do a WRQ


File Already Exists:
----------------------
On the Client:
  When performing a Read Transfer, we first check whether a file with the requested filename
  already exists on the client. If it does, the user is notified with an error message and
  we DO NOT send a RRQ to the server.

On the Server:
  When receiving a WRQ the server checks whether the file already exists. If it does we send
  a  "File already exists" error (Code 6) and abort the transfer.
  This also resolves issues if two clients try to write the same file at the same time. Only
  one of them (whoever happens to be served first), will be able to write.

Testing:
  - Client:
    - Try to do a RRQ entering a file that already exists on the client.
  - Server:
    - Try to do a WRQ sending a file that already exists on the Server.





==============================================
DESIGN DECISIONS
==============================================


Overall Design:
---------------------
After grappling with many subtle bugs, related to state inconsistencies in previous iterations,
we decided to scrap most of the existing code on the Client and Server and start fresh.

The goal was to minimize state to only the bare essentials. Thus we introduced a "TransferState"
class that encapsulates all state needed for a particular transfer (Read or Write). Furthermore,
every instance of TransferState is immutable, which makes it perfectly safe to pass it around to
many different methods/functions. Since it cannot be changed along the way, it has become much
simpler to keep track and ensure correctness of state.

By removing state completely from the behaviour we were able to break out the different steps
involved in a transfer (receive an ack, read from file, create data packet, send data packet ,...)
into small simple procedures. We opted to use Java 8's new Lambda implementation for most of them
since it allows functions to be treated as values, along with a number of other benefits.
Most of these small functions take TransferState as a parameter and return same as a result.

By completely decoupling state from behaviour, and moving the behaviour required for a transfer
into small re-usable functions, we were able to break out all of that behaviour into its own package.
(Utils/tftp_transfer). 

Clearly, at some point state does have to change, e.g. once we read a block from a file. In that
case a function will ALWAYS return a new state object containing new information. Every function
is designed to only change a single aspect of the state, thus most of the old state can be cloned 
and then new values are assigned to the new state object.
Creating a new state object for every change may seem inefficient, but as it turns out, Java is
pretty efficient in creating objects (who knew!?). Also, the state object is pretty small, containing
only a few primitive types, and a number of references to other objects. These references are
simply copied over to the new state object and do not require to re-instantiate the objects
they refer to.
Also, the benefits by far outweigh the negatives as we'll see below.

As it turns out, performing a TFTP Read Transfer on the Server is almost the exact same as performing 
a TFTP Write Transfer on the Client. The same is true for Write Transfer on the Server and Read 
Transfer on the Client.
Thus, we are now sharing a majority of that behaviour between Client and Server. So client and server
both run pretty much the same code to facilitate a Tftp Transfer.

As such we have reached epic proportions of code reuse and maintainability.


Note on ROP:
-------------
Railway Oriented Programming (ROP) is a new-ish concept used in Functional Programming that can aid in error
handling. Essentially, all functions return a Result type which can either be a SUCCESS or a FAILURE. 
When composing functions, as soon as one of them returns a FAILURE result, the rest of them are 
bypassed. The error is then handled in a single place afterwards. This makes the code much cleaner 
and removes the need for exceptions in that situation.
The Utils/rop package contains some helper functions to facilitate ROP in our project.

Source: http://fsharpforfunandprofit.com/posts/recipe-part2/





