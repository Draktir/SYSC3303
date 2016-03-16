# TODO - Iteration 04

## Outstanding tasks

6. ReadMe.txt needs to be updated to reflect new changes.
7. ClassDiagrams needs to be updated to reflect new changes.
8. TimingDiagrams needs to be updated to reflect new changes.
9. Explain decisions we've made in README (not overwriting files, etc.)
10. Use Logger class everywhere
11. Check to make sure ErrorPacket modifications work on Intermediate host
12. Choosing exit [0] in Configuration menu does not work
13. Make file path on server and client configurable

## DONE - needs testing

1. Error Code 01 (File not found) needs to be implemented.
2. Error Code 02 (Access violation) needs to be implemented.
3. Error Code 03 (Disk full/allocation exceeded) needs to be implemented.
4. Error Code 06 (File already exists) needs to be implemented. 
5. Make sure Intermediate Host is sufficient in providing test cases for the new error codes.

## DONE - tested

# Deliverables for Iteration 04

* “README.txt” file explaining the names of your files, set up instructions, etc.
* Breakdown of responsibilities of each team member for this and previous iterations
* Any unchanged diagrams from the previous iterations
* UML class diagram
* Timing diagrams showing the error scenarios for this iteration
* Detailed set up and test instructions, including test files used
* Code (.java files, all required Eclipse files, etc.)

# Error Scenarios

## Error Code 00 (Undefined error)
* This does not need to be implemented, but may be useful for sending miscellaneous error messages.

## Error Code 01 (File not found)
* The file name provided by the request could not be found by the client/server.

## Error Code 02 (Access violation)
* The client/server does not have the appropriate permissions to read/write from/to a file or directory.

## Error Code 03 (Disk full/allocation exceeded)

## Error Code 06 (File already exists)
* TFTP does not specify what happens if this should be the case.
  * We could let the user choose to overwrite the file or not.




TFTP Spec: http://tools.ietf.org/html/rfc1350
