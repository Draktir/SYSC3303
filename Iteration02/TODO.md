# TODO - Iteration 02

## Outstanding tasks

1. (L) Format debugging output using indentation (`\t`) and separate connections

4. Intermediate Host
  1. Menu to pick an error scenario and which packet to modify
  2. Parse all packets and introduce error where applicable
  3. "We should be able to simulate any problem with any packet and any field within any packet."

9. (F) Add a Config class that has all constants (Ports, etc.)
10. (A)Timing diagrams showing the error scenarios for this iteration
11. NICE TO HAVE: add TEST mode


## DONE - needs testing
3. (P) Server error detection & handling 
  1. Increase buffer size to 517 bytes to check whether the packet is too long
8. (P) Review parser and builder to make sure all errors are caught
2. Client error detection & handling
  1. Increase buffer size to 517 bytes to check whether the packet is too long

## DONE - tested
7. ~~(P) Package Packet and File stuff into libraries for reuse (or something like that)~~
5. ~~(A) Improve client menu (let user enter any filename)~~
  ~~1. Check the file isn't too big, max size: (512 * 2^16) - 1 bytes~~
6. (L) Server shutdown needs to be fixed (program does not "terminate", but no longer listens for new connections, and will allow current transfers to finish)


# Deliverables for Iteration 2

* “README.txt” file explaining the names of your files, set up instructions, etc.
* Breakdown of responsibilities of each team member for this and previous iterations
* Any unchanged diagrams from the previous iterations
* UML class diagram
* Timing diagrams showing the error scenarios for this iteration
* Detailed set up and test instructions, including test files used
* Code (.java files, all required Eclipse files, etc.)


# Error Scenarios

## Error Code 4 (Illegal TFTP operation)

On any of these errors, send the error packet, then close the socket and terminate the thread (unless it’s port 69, then we send an error and ignore the packet).

* Invalid ReadRequest
  * malformed packet: consult TFTP spec
  * missing filename
  * missing mode
  * wrong mode
* Invalid WriteRequest
  * malformed packet: consult TFTP spec
  * missing filename
  * missing mode
  * wrong mode
* Invalid DataPacket
  * malformed packet: consult TFTP spec
  * missing block#
  * out of order block#
  * too long (> 516 bytes)
  * too short (< 4 bytes)
* Invalid Acknowledgement
  * out of order block#
  * too short (< 4 bytes)
  * too long (> 4 bytes)
* Other
  * Server receives something other than a READ or WRITE request on Port 69.
  * Packet is completely screwed (e.g. starts with 0 6+)


## Error Code 5 (Unknown transfer ID)

If an error 5 occurs, we send an error packet and ignore the received packet. Do not terminate.

The port the other end sent the packet from must match the previous port number. (TID of other host must remain the same for the entire duration of the transfer).


TFTP Spec: http://tools.ietf.org/html/rfc1350
