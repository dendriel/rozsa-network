package com.rozsa.network;

import com.rozsa.network.message.DisconnectedMessage;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.EnumMap;

public class PeerLoop extends Thread implements PacketSender {
    private final UDPSocket udpSocket;
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue incomingMessages;
    private final CachedMemory cachedMemory;
    private final int recvMessagesThreshold;

    private EnumMap<MessageType, IncomingMessageHandler> messageHandlers;
    private volatile boolean isRunning;

    PeerLoop(ConnectionHolder connHolder,
            IncomingMessagesQueue incomingMessages,
            CachedMemory cachedMemory,
            PeerConfig config,
            int recvMessagesThreshold
    ) throws SocketException {

        this.connHolder = connHolder;
        this.incomingMessages = incomingMessages;
        this.cachedMemory = cachedMemory;
        isRunning = true;
        initializeHandlers();

        this.recvMessagesThreshold = recvMessagesThreshold;

        udpSocket = new UDPSocket(config.getPort(), 1, config.getReceiveBufferSize(), cachedMemory);
    }

    private void initializeHandlers() {
        messageHandlers = new EnumMap<>(MessageType.class);
        messageHandlers.put(MessageType.UNKNOWN, new UnknownMessageHandler());
        messageHandlers.put(MessageType.CONNECTION_REQUEST, new ConnectionRequestHandler(connHolder, incomingMessages, this));
        messageHandlers.put(MessageType.CONNECTION_RESPONSE, new ConnectionResponseHandler(connHolder, incomingMessages, this));
        messageHandlers.put(MessageType.CONNECTION_ESTABLISHED, new ConnectionEstablishedHandler(connHolder, incomingMessages));
        messageHandlers.put(MessageType.CONNECTION_DENIED, new ConnectionDeniedHandler());
        messageHandlers.put(MessageType.CONNECTION_CLOSED, new ConnectionClosedHandler(connHolder));
        messageHandlers.put(MessageType.PING, new PingMessageHandler(connHolder));
        messageHandlers.put(MessageType.PONG, new PongMessageHandler(connHolder));
        messageHandlers.put(MessageType.ACK, new AckMessageHandler(connHolder));
        messageHandlers.put(MessageType.USER_DATA, new UserDataHandler(connHolder, incomingMessages));
    }

    public void run() {
        while (isRunning) {
            try {
                loop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loop() {
        int currRecvMsgsCount = 0;
        boolean noMoreMessages;
        do {
            noMoreMessages = receivePackets();
        } while (!noMoreMessages && ++currRecvMsgsCount < recvMessagesThreshold);

        connHolder.getHandshakes().forEach(this::expireHandshakes);
        connHolder.getHandshakes().forEach(Connection::handshake);

        connHolder.getConnections().forEach(Connection::update);
        connHolder.getConnections().forEach(this::handleDisconnect);
    }

    // do not call outside peer loop thread to avoid UDPSocket concurrency.
    public void send(Address addr, byte[] data, int dataLen) {
        Logger.debug("Sending " + MessageType.from(data[0]) + " to " + addr);
        udpSocket.send(addr.getNetAddress(), addr.getPort(), data, dataLen);
    }

    private void expireHandshakes(Connection conn) {
        if (conn.isHandshakeExpired()) {
            connHolder.removeHandshake(conn);
            DisconnectedMessage disconnectedMessage = new DisconnectedMessage(conn, DisconnectReason.NO_RESPONSE);
            incomingMessages.enqueue(disconnectedMessage);
        }

        if (conn.isAwaitingConnectionEstablishedExpired()) {
            // may concur with user thread trying to connect to this address. [?!]
            connHolder.removeHandshake(conn);
        }
    }

    private void handleDisconnect(Connection conn) {
        if (conn.getState() != ConnectionState.DISCONNECTED) {
            return;
        }
        connHolder.removeConnection(conn);

        DisconnectReason reason = conn.getDisconnectReason();
        DisconnectedMessage disconnectedMessage = new DisconnectedMessage(conn, reason);
        incomingMessages.enqueue(disconnectedMessage);

        if (reason == DisconnectReason.LOCAL_CLOSE) {
            byte[] buf = MessageSerializer.serialize(MessageType.CONNECTION_CLOSED);
            send(conn.getAddress(), buf, buf.length);
        }
    }

    /**
     * Read a message from socket.
     * @return true if socket timeout occurred and there is no more messages to read right now; false otherwise.
     */
    private boolean receivePackets() {
        DatagramPacket packet = udpSocket.receive();
        if (packet == null) {
            // Socket timeout. It won't burn the CPU.
            return true;
        }

        Address addr = Address.from(packet.getAddress(), packet.getPort());
        int dataIdx = 0;
        byte[] buf = packet.getData();
        int length = packet.getLength();
        if (length <= 0) {
            return false;
        }

        // deserialize header
        MessageType type = MessageType.from(buf[dataIdx++]);
        DeliveryMethod method = DeliveryMethod.from(buf[dataIdx++]);
        int seqNumber = (buf[dataIdx++] & 0xFF);
        seqNumber = (seqNumber | (buf[dataIdx++] & 0xFF) << 8);

        // deserialize data
        int dataLen = length - dataIdx;
        byte[] data = cachedMemory.allocBuffer(dataLen);
        System.arraycopy(buf, dataIdx, data, 0, dataLen);

        IncomingMessageHandler handler = messageHandlers.getOrDefault(type, new UnknownMessageHandler());
        handler.handle(addr, method, (short)seqNumber, data, dataLen);

        return false;
    }
}
