Design Decisions
===========================

File Not Found:
---------------------
There are two possible scenarios in which this error may occur:

1) The server received a RRQ but the filename contained in the request cannot be found.
   In that case the server sends an error (Code 1) and aborts the transfer.

2) On the server, if during a Read Transfer, the file we are reading from is (re)moved,
   the server cannot continue to read from it and thus will send an Error Code 1, and 
   abort the transfer.
   On the client, if during a Write Transfer, the file we are reading from is (re)moved 
   the Client sends an Error Code 1, and aborts the transfer.

   Note that we are using a Buffered Input Stream, so for small files this will not
   occur, since the entire file will already be in the buffer.


Access Violation:
--------------------
On the Client:
   If, on a Write Transfer, the file the user selects cannot be read, the user is informed
   a with an error message and we DO NOT send a WRQ to the server.
   If during a Read Transfer, on receipt of the first DataPacket the file cannot be created, 
   the Client sends an Error code 2 to the server and terminates the transfer, displaying
   the error to the user.

On the Server:
   If trying to read or write a file is not possible because the permissions are insufficient
   (no read/write access), the server responds with an Error code 2 and terminates the transfer.

Note that if the permissions change during a transfer, it does not affect the program, since it 
already opened the file and has a valid file descriptor. Thus it can continue to read/write and
complete the transfer.


Disk Full:
---------------------
If trying to write a block to a file on disk fails, because the disk is full, both client
and server will respond in the same way. First an error message (Code 3) is sent and then
the file that had been written to is deleted, since it is incomplete.


File Alread Exists:
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
Railway Oriented Programming (ROP) is a concept used in Functional Programming that can aid in error
handling. Essentially, all functions return a Result type which can either be a SUCCESS or a FAILURE. 
When composing functions, as soon as one of them returns a FAILURE result, the rest of them are 
bypassed. The error is then handled in a single place afterwards. This makes the code much cleaner 
and removes the need for exceptions in that situation.
The Utils/rop package contains some helper functions to facilitate ROP in our project.

Source: http://fsharpforfunandprofit.com/posts/recipe-part2/





