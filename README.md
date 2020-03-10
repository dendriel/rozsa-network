# Rozsa Network
Simple Reliable UDP (RUDP) com.rozsa.network library in Java.

# Features

- Three-way handshake (connection request, response and established);
- Connection Heartbeat (provides latency value);
- Latency monitoring (uses weighted average to calculate latency);
- Latency update event (value in micros and millis);
- Connection status events (connected; disconnected);
- Disconnection reason;
- Synchronized network events delivery (NetworkPeer.read() may return a data message or execute an event method);
- Data delivery;
- Data buffer recycling.

# Delivery Methods

- Unreliable;
- Reliable.

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
  - Reliable sequenced;
  - Reliabled ordered. 
 - Add channels [?];
 - Add message coalescing;
 - Add fragmentation;
 - Review header space usage.
