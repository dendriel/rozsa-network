# Rozsa Network
Simple Reliable UDP (RUDP) com.rozsa.network library in Java.

# Features

- Three-way handshake (connection request, response and established);
- Connection Heartbeat (provides latency value);
- Latency monitoring (uses weighted average to calulate latency).
- Data delivery.

# Delivery Methods

- Unreliable

# Protocol Header

4 bytes size:

```
1 byte - message type
1 byte - delivery method
2 bytes - sequence number
```

# TODO

- Add unreliable delivery methods:
 - Unreliable sequenced.
- Add reliable delivery methods:
  - Reliable;
  - Reliable sequenced;
  - Reliabled ordered. 
 - Add channels [?]
