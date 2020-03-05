package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.message.incoming.DisconnectedMessage;
import com.rozsa.network.message.outgoing.*;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.EnumMap;

public class PeerLoop extends Thread implements PacketSender {
    private final UDPSocket udpSocket;
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue incomingMessages;
    private EnumMap<MessageType, IncomingMessageHandler> messageHandlers;

    private volatile boolean isRunning;

    PeerLoop(ConnectionHolder connHolder, IncomingMessagesQueue incomingMessages, PeerConfig config) throws SocketException {
        this.connHolder = connHolder;
        this.incomingMessages = incomingMessages;
        isRunning = true;
        initializeHandlers();

        udpSocket = new UDPSocket(config.getPort(), 1, config.getReceiveBufferSize());
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
        messageHandlers.put(MessageType.USER_DATA, new UserDataHandler(connHolder, incomingMessages));
    }

    public void run() {
        while (isRunning) {
            try {
                loop();
//                Thread.sleep(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void loop() {
        receivePackets();

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
            ConnectionClosedMessage closedMessage = new ConnectionClosedMessage();
            send(conn.getAddress(), closedMessage.getData(), closedMessage.getDataLength());
        }
    }

    private void receivePackets() {
        DatagramPacket packet = udpSocket.receive();
        if (packet == null) {
            // Socket timeout. It won't burn the CPU.
            return;
        }

        Address addr = Address.from(packet.getAddress(), packet.getPort());
        int dataIdx = 0;
        byte[] buf = packet.getData();
        int length = packet.getLength();
        if (length <= 0) {
            return;
        }

        // deserialize header
        MessageType type = MessageType.from(buf[dataIdx++]);
        DeliveryMethod method = DeliveryMethod.from(buf[dataIdx++]);
        int seqNumber = (buf[dataIdx++] & 0xFF);
        seqNumber = (seqNumber | (buf[dataIdx++] & 0xFF) << 8);
//        Logger.info("SEQ %d", seqNumber);
//        Logger.info("RECV %s", type);

        // deserialize data
        byte[] data = new byte[length - dataIdx];
        System.arraycopy(buf, dataIdx, data, 0, data.length);

        IncomingMessageHandler handler = messageHandlers.getOrDefault(type, new UnknownMessageHandler());
        handler.handle(addr, method, (short)seqNumber, data, data.length);
    }
}
