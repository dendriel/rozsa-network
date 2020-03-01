package com.rozsa.network;

import com.rozsa.network.message.incoming.DisconnectedMessage;
import com.rozsa.network.message.outgoing.*;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.EnumMap;

public class PeerLoop extends Thread implements PacketSender {
    private final UDPSocket udpSocket;
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue messageQueue;
    private final PeerConfig config;
    private EnumMap<MessageType, IncomingMessageHandler> messageHandlers;

    private volatile boolean isRunning;

    PeerLoop(ConnectionHolder connHolder, IncomingMessagesQueue messageQueue, PeerConfig config) throws SocketException {
        this.connHolder = connHolder;
        this.messageQueue = messageQueue;
        this.config = config;
        isRunning = true;
        initializeHandlers();

        udpSocket = new UDPSocket(config.getPort(), 1, config.getReceiveBufferSize());
    }

    private void initializeHandlers() {
        messageHandlers = new EnumMap<>(MessageType.class);
        messageHandlers.put(MessageType.CONNECTION_REQUEST, new ConnectionRequestHandler(connHolder, messageQueue, this));
        messageHandlers.put(MessageType.CONNECTION_RESPONSE, new ConnectionResponseHandler(connHolder, messageQueue, this));
        messageHandlers.put(MessageType.CONNECTION_ESTABLISHED, new ConnectionEstablishedHandler(connHolder, messageQueue));
        messageHandlers.put(MessageType.CONNECTION_DENIED, new ConnectionDeniedHandler());
        messageHandlers.put(MessageType.CONNECTION_CLOSED, new ConnectionClosedHandler(connHolder));
        messageHandlers.put(MessageType.PING, new PingMessageHandler(connHolder));
        messageHandlers.put(MessageType.USER_DATA, new UserDataHandler(connHolder, messageQueue));
        messageHandlers.put(MessageType.UNKNOWN, new UnknownMessageHandler());
    }

    public void run() {
        while (isRunning) {
            try {
                loop();
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void loop() {
        receivePackets();

        handleExpiredHandshakes();
        handleHandshakes();

        handleUpdates();
        handleDisconnects();
    }

    // do not call outside peer loop thread to avoid UDPSocket concurrency.
    public void send(Address addr, byte[] data, int dataLen) {
        Logger.info("Sending " + MessageType.from(data[0]) + " to " + addr);
        udpSocket.send(addr.getNetAddress(), addr.getPort(), data, dataLen);
    }

    private void handleExpiredHandshakes() {
        connHolder.getHandshakes().forEach(this::expireHandshakes);
    }

    private void expireHandshakes(Connection conn) {
        if (conn.isHandshakeExpired()) {
            connHolder.removeHandshake(conn);
            DisconnectedMessage disconnectedMessage = new DisconnectedMessage(conn, DisconnectReason.NO_RESPONSE);
            messageQueue.enqueue(disconnectedMessage);
        }

        if (conn.isAwaitingConnectionEstablishedExpired()) {
            // may concur with user thread trying to connect to this address. [?!]
            Logger.info("Handshake expired while waiting for connection established from %s.", conn);
            connHolder.removeHandshake(conn);
        }
    }

    private void handleDisconnects() {
        connHolder.getConnections().forEach(this::handleDisconnect);
    }

    private void handleDisconnect(Connection conn) {
        if (conn.getState() != ConnectionState.DISCONNECTED) {
            return;
        }
        connHolder.removeConnection(conn);

        DisconnectReason reason = conn.getDisconnectReason();
        DisconnectedMessage disconnectedMessage = new DisconnectedMessage(conn, reason);
        messageQueue.enqueue(disconnectedMessage);

        if (reason == DisconnectReason.LOCAL_CLOSE) {
            ConnectionClosedMessage closedMessage = new ConnectionClosedMessage();
            send(conn.getAddress(), closedMessage.serialize(), closedMessage.getDataLength());
        }
    }

    private void handleHandshakes() {
        connHolder.getHandshakes().forEach(Connection::handshake);
    }

    private void handleUpdates() {
        connHolder.getConnections().forEach(Connection::update);
    }

    private void receivePackets() {
        DatagramPacket packet = udpSocket.receive();
        if (packet == null) {
            // Socket timeout. It won't burn the CPU.
            return;
        }

        Address addr = Address.from(packet.getAddress(), packet.getPort());
        int dataIdx = 0;
        byte[] data = packet.getData();
        MessageType type = MessageType.from(data[dataIdx++]);

        Logger.info("Received %s %s", type, addr);

        IncomingMessageHandler handler = messageHandlers.getOrDefault(type, new UnknownMessageHandler());
        handler.handle(addr, data, dataIdx);
    }
}
