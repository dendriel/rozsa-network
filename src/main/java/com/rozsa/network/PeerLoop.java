package com.rozsa.network;

import com.rozsa.network.message.incoming.DisconnectedMessage;
import com.rozsa.network.message.outgoing.*;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Collection;
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
        messageHandlers.put(MessageType.USER_DATA, new UserDataHandler(connHolder, messageQueue));
        messageHandlers.put(MessageType.CONNECTION_DENIED, new ConnectionDeniedHandler());
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
        handleExpiredHandshakes();
        handleHandshakes();
        handleUpdates();

        receivePackets();
    }

    public void send(Address addr, byte[] data, int dataLen) {
        Logger.info("Sending " + MessageType.from(data[0]) + " to " + addr);
        udpSocket.send(addr.getNetAddress(), addr.getPort(), data, dataLen);
    }

    private void handleExpiredHandshakes() {
        Collection<Connection> handshakes = connHolder.getHandshakes();
        for (Connection conn : handshakes) {
            if (conn.isHandshakeExpired()) {
                removeExpiredHandshake(conn);
            }

            if (conn.isAwaitingConnectionEstablishedExpired()) {
                // may concur with user thread trying to connect to this address. [?!]
                Logger.info("Handshake expired while waiting for connection established from %s.", conn);
                connHolder.removeHandshake(conn);
            }
        }
    }

    private void removeExpiredHandshake(Connection conn) {
        connHolder.removeHandshake(conn);
        DisconnectedMessage disconnectedMessage = new DisconnectedMessage(conn, DisconnectReason.NO_RESPONSE);
        messageQueue.enqueue(disconnectedMessage);
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
            // Socket timeout. Won't burn the CPU.
            return;
        }

        Address addr = Address.from(packet.getAddress(), packet.getPort());
        int dataIdx = 0;
        byte[] data = packet.getData();
        MessageType type = MessageType.from(data[dataIdx++]);

        handleIncomingMessage(type, addr, data, dataIdx);
    }

    private void handleIncomingMessage(MessageType type, Address addr, byte[] data, int dataIdx) {
        Logger.info("Received " + type + " from " + addr);

        IncomingMessageHandler handler = messageHandlers.getOrDefault(type, new UnknownMessageHandler());
        handler.handle(addr, data, dataIdx);
    }
}
