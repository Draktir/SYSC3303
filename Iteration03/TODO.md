# TODO - Iteration 03

## Outstanding tasks
1. TFTP's protocol for handling timeouts/retransmit needs to be implemented.
  1. If the recipient does not respond within the timeout period, the sender retransmits the last packet.
  2. Delayed packets should not cause termination of the request. 
2. Client needs to handle network errors.
  1. Packets may be lost, delayed, or duplicated.
3. Server needs to handle network errors.
  1. Packets may be lost, delayed, or duplicated.
4. Intermediate Host
  1. Add menu options for testing timeouts/retransmits.
5. Duplicate ACK packets should not be acknowledged.
  1. Only the TID sending DATA packets retransmits after timeout.
6. ReadMe.txt needs to be updated to reflect new changes.
7. ClassDiagrams needs to be updated to reflect new changes.
8. TimingDiagrams needs to be updated to reflect new changes.

## DONE - needs testing

## DONE - tested


# Deliverables for Iteration 2

* “README.txt” file explaining the names of your files, set up instructions, etc.
* Breakdown of responsibilities of each team member for this and previous iterations
* Any unchanged diagrams from the previous iterations
* UML class diagram
* Timing diagrams showing the error scenarios for this iteration
* Detailed set up and test instructions, including test files used
* Code (.java files, all required Eclipse files, etc.)


# Error Scenarios

## Packet is lost, or delayed

If a packet is lost, the sender may retransmit the last packet after the timeout period.

## Duplicate ACK packets received (Sorcerer's Apprentice bug)

Do not send a DATA packet in response, instead terminate the connection.
* TFTP Specification, page 7, paragraph 3.

TFTP Spec: http://tools.ietf.org/html/rfc1350
