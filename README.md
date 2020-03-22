# Rozsa Network
Simple Reliable UDP (RUDP) com.rozsa.network library in Java made by me =].

# Features

- Three-way handshake (connection request, response and established);
- Connection Heartbeat (provides latency value);
- Latency monitoring (uses weighted average to calculate latency);
- Latency update event (value in micros and millis);
- Connection status events (connected; disconnected);
- Disconnection reason;
- Synchronized network events delivery (NetworkPeer.read() may return a data message or execute an event method);
- Data delivery;
- 32 channels for each reliable and sequenced delivery method;
- Flow control for reliable delivery methods (uses a sliding window);
- Data buffer recycling.

# Delivery Methods

- Unreliable:
  - Pure Unreliable;
  - Unreliable Sequenced.
- Reliable:
  - Pure Reliable;
  - Reliable Sequenced;
  - Reliable Ordered.

# Protocol Header

4 bytes size:

```
1 byte - message type
1 byte - delivery method
2 bytes - sequence number
```

# Channels

Effectively, a channel is a tag that one can check in an incoming message to sort the message when processing it. So we have:

- 32 unreliable sequenced channels;
- 32 reliable sequenced channels;
- 32 reliable ordered channels.

To use a channel just pass its ID when sending a message using one of the above delivery methos:
```
// send an unreliable sequenced message at channel 7.
peer.sendMessage(conn, outgoingMsg, DeliveryMethod.UNRELIABLE_SEQUENCED, 7);

// send an reliable sequenced message at channel 31.
peer.sendMessage(conn, outgoingMsg, DeliveryMethod.RELIABLE_SEQUENCED, 31);

// send an reliable sequenced message at channel 0.
peer.sendMessage(conn, outgoingMsg, DeliveryMethod.RELIABLE_ORDERED);
```

*When the channel is omitted, channel 0 is automatically used.

# TODO

- Add message coalescing;
- Add fragmentation;
- Review header space usage:
  - Sequence numbers doesn't use all 16 bits.
- Add testbed to test delivery methods;
- Peer and connection statistics (peer and connection):
  - Bytes sent/received;
  - Messages send/received.
- Add remote time offset calculation.
