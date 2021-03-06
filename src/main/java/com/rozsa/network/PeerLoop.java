package com.rozsa.network;

import com.rozsa.network.message.DisconnectedMessage;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.EnumMap;

public class PeerLoop implements PacketSender {
    private final UDPSocket udpSocket;
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue incomingMessages;
    private final CachedMemory cachedMemory;
    private final int recvMessagesThreshold;
    private final UserDataHandler userDataHandler;

    private EnumMap<MessageType, IncomingMessageHandler> messageHandlers;
    private boolean isStarted;
    private volatile boolean isRunning;

    private Thread threadLoop;

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
        initializeHandlers(config.isConnectionApprovalRequired());

        userDataHandler = new UserDataHandler(connHolder, cachedMemory, incomingMessages, this);
        this.recvMessagesThreshold = recvMessagesThreshold;

        udpSocket = new UDPSocket(config.getPort(), 1, config.getReceiveBufferSize());
    }

    private void initializeHandlers(boolean isApprovalRequired) {
        messageHandlers = new EnumMap<>(MessageType.class);
        messageHandlers.put(MessageType.UNKNOWN, new UnknownMessageHandler(cachedMemory));
        messageHandlers.put(MessageType.CONNECTION_REQUEST, new ConnectionRequestHandler(connHolder, cachedMemory, incomingMessages, this, isApprovalRequired));
        messageHandlers.put(MessageType.CONNECTION_RESPONSE, new ConnectionResponseHandler(connHolder, cachedMemory, incomingMessages, this));
        messageHandlers.put(MessageType.CONNECTION_ESTABLISHED, new ConnectionEstablishedHandler(connHolder, cachedMemory, incomingMessages));
        messageHandlers.put(MessageType.CONNECTION_DENIED, new ConnectionDeniedHandler(connHolder, cachedMemory, incomingMessages));
        messageHandlers.put(MessageType.CONNECTION_CLOSED, new ConnectionClosedHandler(connHolder, cachedMemory));
        messageHandlers.put(MessageType.PING, new PingMessageHandler(connHolder, cachedMemory));
        messageHandlers.put(MessageType.PONG, new PongMessageHandler(connHolder, cachedMemory));
        messageHandlers.put(MessageType.ACK, new AckMessageHandler(connHolder, cachedMemory));
    }

    public void start() {
        if (isStarted) {
            return;
        }

        isStarted = true;
        isRunning = true;
        threadLoop = new Thread(this::run);
        threadLoop.start();
    }

    public void stop() {
        if (!isStarted) {
            return;
        }

        try {
            isRunning = false;
            threadLoop.join(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        disconnectAllImmediately();
        isStarted = false;
    }

    private void run() {
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
    public void send(Address addr, byte[] data, int dataLen, boolean freeData) {
        udpSocket.send(addr.getNetAddress(), addr.getPort(), data, dataLen);

        if (freeData) {
            cachedMemory.freeBuffer(data);
        }
    }

    public void sendProtocol(Address addr, MessageType type, short seqNumber) {
        byte[] buf = cachedMemory.allocBuffer(NetConstants.MsgHeaderSize);

        int bufIdx = 0;
        buf[bufIdx++] = type.getId();
        buf[bufIdx++] = (byte)(seqNumber << 1);
        buf[bufIdx] = (byte)(seqNumber >> 7);
        udpSocket.send(addr.getNetAddress(), addr.getPort(), buf, NetConstants.MsgHeaderSize);

        cachedMemory.freeBuffer(buf);
    }

    /**
     * Useful when we want to send data alongside a protocol message (like a hail message in a connection request).
     */
    public void encodeSendProtocol(Address addr, MessageType type, short seqNumber, byte[] data, int dataLen) {
        int bufSize = dataLen + NetConstants.MsgHeaderSize;
        byte[] buf = cachedMemory.allocBuffer(bufSize);
        int bufIdx = 0;
        buf[bufIdx++] = type.getId();
        buf[bufIdx++] = (byte)((seqNumber << 1));
        buf[bufIdx++] = (byte)(seqNumber >> 7);

        System.arraycopy(data, 0, buf, bufIdx, dataLen);

        send(addr, buf, bufSize, true);
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

        if (conn.isDisconnected()) {
            // incoming connection was refused by this peer.
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
            sendProtocol(conn.getAddress(), MessageType.CONNECTION_CLOSED, (short)0);
        }
    }

    private void disconnectAllImmediately() {
        for (Connection conn : connHolder.getConnections()) {
            conn.setDisconnected(DisconnectReason.LOCAL_CLOSE);
            handleDisconnect(conn);
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

        byte low = buf[dataIdx++];
        byte high = buf[dataIdx++];

        boolean isFrag = ((low & 0x1) == 1);
        int seqNumber = ((((low & 0xFF) >> 1) & 0xFF) | ((high & 0xFF) << 7));

        // deserialize data
        int dataLen = length - dataIdx;
        byte[] data = cachedMemory.allocBuffer(dataLen);
        System.arraycopy(buf, dataIdx, data, 0, dataLen);

        IncomingMessageHandler handler = messageHandlers.getOrDefault(type, userDataHandler);
        handler.handle(addr, type, (short)seqNumber, data, dataLen, isFrag);

        return false;
    }
}
