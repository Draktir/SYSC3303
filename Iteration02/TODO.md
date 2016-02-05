# TODO - Iteration 02

1. Format debugging output using indentation (\\t) and separate connections
2. Client error detection & handling
  2.1. Increase buffer size to 517 bytes to check whether the packet is too long
3. Server error detection & handling
  3.1. Increase buffer size to 517 bytes to check whether the packet is too long
4. Intermediate Host
  4.1. Menu to pick an error scenario and which packet to modify
  4.2. Parse all packets and introduce error where applicable
5. Improve client menu (let user enter any filename)
6. Server shutdown needs to be fixed (probably needs to check Thread.currentThread().isInterrupted())
7. Package Packet and File stuff into libraries for reuse (or something like that)
8. Add a Config class that has all constants (Ports, etc.)
