# Rozsa Network
Reliable UDP (RUDP) com.rozsa.network library in Java =].

# Features

- Three-way handshake (connection request, response and established);
- Connection approval (host may approve or deny an incoming connection);
- Connection Heartbeat (provides latency value);
- Latency monitoring (uses weighted average to calculate latency);
- Latency update event (value in micros and millis);
- Connection status events (connected; disconnected);
- Connection/Disconnection events flow;
- Disconnection reason;
- Synchronized network events delivery (NetworkPeer.read() may return a data message or execute an event method);
- Data delivery;
- Data fragmentation for reliable delivery methods;
- 32 channels for each reliable and sequenced delivery method;
- Flow control for reliable delivery methods (uses a sliding window);
- Data buffer recycling;
- Types serialization/deserialization;
- Message loop sending (send a message to itself - useful for testing).

# Delivery Methods

- Unreliable:
  - Pure Unreliable;
  - Unreliable Sequenced.
- Reliable:
  - Pure Reliable;
  - Reliable Sequenced;
  - Reliable Ordered.

# Protocol Header

3 bytes size when unfragmented; 9 bytes size when fragmented:

```
8 bits - message type
15 bits - sequence number
1 bit - is fragment?
-- fragmentation part
16 bits - fragment group;
16 bits - fragment total length;
16 bits - fragment offset.
```

# Connection/Disconnection events flow

The library tries to ensure that a connection event will always be followed by a disconnection event
(when remote peer disconnects). That is, the same address will never be allowed to reconnect
while having an active connection. The first connection must close (timeout or graceful disconnection)
before a new connection for the same address is accepted.

To get along with this behavior (when graceful disconnection is not possible), it may be interesting
to have a handshake interval larger than a connection timeout interval. This way, if a connection
request happens while another connection from the same peer is still active, the active connection
may timeout (and disconnects) before the new connection give up trying to connect.

# Channels

Effectively, a channel is a tag that one can check in an incoming message to sort the message when processing it. So we have:

- 32 unreliable sequenced channels;
- 32 reliable sequenced channels;
- 32 reliable ordered channels.

To use a channel just pass its ID when sending a message using one of the above delivery methos:
```Java
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

# Types Serialization and Deserialization
To send a message, one can get the data buffer (byte[]) and write/read directly into/from it. Another approach
of serializing data is to use some facilities to write types directly into the message.

For example:
```Java
// Send (write)
OutgoingMessage outgoingMsg = peer.createOutgoingMessage(10);
outgoingMsg.writeByte((byte)101);
outgoingMsg.writeInt(900000);
outgoingMsg.writeString("Rozsa Network");
peer.sendMessage(conn, outgoingMsg, DeliveryMethod.RELIABLE_SEQUENCED);

// Receive (read)
IncomingUserDataMessage msg = peer.read();
System.out.println(msg.readByte());   // prints "101"
System.out.println(msg.readInt());    // prints "900000"
System.out.println(msg.readString()); // prints "Rozsa Network"
```

Handled types right now:

- byte;
- byte[];
- int;
- short;
- long;
- float;
- boolean;
- BitSet;
- String.

Writing types directly into the message easies testing and may be fit for some type of messages (like authentication message). However, some writeable types adds some payload overhead. For example, the byte array and string writing adds two extra bytes used to track their lengths in reading operations. 

# Peer Configurations

Peer configuration is provided by PeerConfig class and is used by both client and server. Default values are retrieved from NetConstants.

```Java
/**
 * UDP port to connect to (client) or listen to (server).
 */
int port;

/**
 * Maximum handshakes attempts while trying to connect to a peer.
 */
int maximumHandshakeAttempts;

/**
 * Interval in milliseconds before handshake retry.
 */
long intervalBetweenHandshakes;

/**
 * Maximum size of UDP data receiving buffer.
 */
int receiveBufferSize;

/**
 * Ping (heartbeat) interval. Also used to calculate latency.
 */
float pingInterval;

/**
 * Maximu waiting time to consider connection lost after not receiving any pings from connected peer.
 */
float connectionTimeout;

/**
 * Enable connection latency report when updated.
 */
boolean isPingUpdatedEventEnabled;

/**
 * Maximum number of byte[] to keep in cache. (if maximum is reached, some random entry will be removed to make
 * room to new cache entry).
 */
int maxCachedBufferCount;

/**
 * Maximum transfer unit (UDP packet) without couting the RUDP header. If you set this wrong, some user may have
 * connection issues due to internet providers dropping packets. Also, use getMaxUserPayload() to subtract the RUDP
 * header size.
 */
int mtu;

/**
 * Enable connection approval events.
 */
boolean connectionApprovalRequired;
```

PeerConfig usage example:

```Java
PeerConfig config = new PeerConfig(targetPort);
config.setPingInterval(1f);
config.setMaximumHandshakeAttempts(10);
config.setIntervalBetweenHandshakes(1000);

peer = new NetworkPeer(config);
```

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
  - Allow to send the same message to multiple connections;
- Add unit tests (and maybe system tests);
- Add Client and Server utility classes.
