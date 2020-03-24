# Rozsa Network
Simple Reliable UDP (RUDP) com.rozsa.network library in Java made by me =].

# Features

- Three-way handshake (connection request, response and established);
- Connection approval (host may approve or deny an incoming connection);
- Connection Heartbeat (provides latency value);
- Latency monitoring (uses weighted average to calculate latency);
- Latency update event (value in micros and millis);
- Connection status events (connected; disconnected);
- Disconnection reason;
- Synchronized network events delivery (NetworkPeer.read() may return a data message or execute an event method);
- Data delivery;
- Data fragmentation for reliable delivery methods;
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
8 bits - message type
15 bits - sequence number
1 bit - is fragment?
16 bits - fragment group;
16 bits - fragment total length;
16 bits - fragment offset.
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

# Fragmentation
Fragmentation is a convenience when we want to send messages larger than the maximum transmission unit. To be
bellow the maximum payload, it is possible to check the maximum user payload by using ``NetworkPeer.getMaxUserPayload()``.
This call will return the maximum amount of bytes that can be sent in a message without fragmentation. It is
lower than the MTU because it discounts the default RUDP header size.

It is possible to increase the MTU by setting it via ``PeerConfig.setMtu(value)``. The default value
is 508 which is a good value to avoid packet drops. Due to keeping the RUDP header size limited, the maximum size of a
message that will be fragmented is 64kB (65536 bytes).

Fragmentation should be used for setup scenarios or very low send ratio situations. It adds 6 extra bytes
to the RUDP header for controlling the multiple fragments and the message may be delayed at the target end
until all fragments arrive.

# Connection Approval

TODO

# TODO

- Add message coalescing;
- Review header space usage:
  - Sequence numbers doesn't use all 16 bits.
- Add testbed to test delivery methods;
- Peer and connection statistics (peer and connection):
  - Bytes sent/received;
  - Messages send/received.
- Add remote time offset calculation;
- Create server and client utility classes
  - Allow to send the same message to multiple connections.
