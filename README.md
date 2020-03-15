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
- Flow control for reliable delivery methods (uses a sliding window);
- Data buffer recycling.

# Delivery Methods

- Unreliable:
  - Pure Unreliable;
  - Unreliable Sequenced.
- Reliable:
  - Pure Reliable;
  - Reliable Sequenced.

# Protocol Header

4 bytes size:

```
1 byte - message type
1 byte - delivery method
2 bytes - sequence number
```

# TODO

- Add reliable delivery methods:
  - Reliable ordered.
- Add channels [?];
- Add message coalescing;
- Add fragmentation;
- Review header space usage:
  - Sequence numbers doesn't use whole all 16 bits.
