package com.rozsa.network;

import com.rozsa.network.message.incoming.ConnectedMessage;
import com.rozsa.network.message.incoming.DisconnectedMessage;
import com.rozsa.network.message.outgoing.ConnectEstablishedMessage;
import com.rozsa.network.message.outgoing.ConnectResponseMessage;
import com.rozsa.network.message.outgoing.MessageType;

import java.net.DatagramPacket;
import java.net.SocketException;
import java.util.Collection;

public class PeerLoop extends Thread implements PacketSender {
    private final UDPSocket udpSocket;
    private final ConnectionHolder connHolder;
    private final IncomingMessageQueue messageQueue;
    private final PeerConfig config;

    private volatile boolean isRunning;

    public PeerLoop(ConnectionHolder connHolder, IncomingMessageQueue messageQueue, PeerConfig config) throws SocketException {
        this.connHolder = connHolder;
        this.messageQueue = messageQueue;
        this.config = config;
        isRunning = true;

        udpSocket = new UDPSocket(config.getPort(), 1, 1500);
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

    public void loop() {
        handleExpiredHandshakes();
        handleHandshakes();
        handleUpdates();
        receiveIncomingPackets();
    }

    public void send(Address addr, byte[] data, int dataLen) {
        Logger.info("Sending outgoing message of " + MessageType.from(data[0]) + " " + addr);
        udpSocket.send(addr.getNetAddress(), addr.getPort(), data, dataLen);
    }

    private void handleExpiredHandshakes() {
        Collection<Connection> handshakes = connHolder.getHandshakes();
        for (Connection conn : handshakes) {
            if (conn.isHandshakeExpired()) {
                removeExpiredHandshake(conn);
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

    private void receiveIncomingPackets() {
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
        Logger.info("Received incoming message of " + type + " " + addr);

        switch (type) {
            case CONNECT_REQUEST:
                handleConnectionRequest(addr, data, dataIdx);
                break;
            case CONNECT_RESPONSE:
                handleConnectionResponse(addr, data, dataIdx);
                break;
            case CONNECT_ESTABLISHED:
                handleConnectionEstablished(addr, data, dataIdx);
                break;
            case CONNECT_DENIED:
                break;
            case USER_DATA:
                handleUserData(addr, data, dataIdx);
                break;
            case UNKNOWN:
            default:
                break;
        }
    }

    private void handleConnectionRequest(Address addr, byte[] data, int dataIdx) {
        Connection conn = connHolder.getHandshake(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        switch (conn.getCtrlState()) {
            case DISCONNECTED:
                // if disconnected, send connect response and await for connect established.
                conn.setCtrlState(ControlConnectionState.AWAITING_CONNECT_ESTABLISHED);
                sendConnectResponse(conn);
                break;

            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
                // if received connect request while sending connect request itself, establish the connection right away.
                messageQueue.enqueue(new ConnectedMessage(conn));
                connHolder.promoteConnection(conn);
                sendConnectEstablished(conn);
                break;

            case CONNECTED:
                Logger.info("Already connected to %s. Resend connect response.", conn);
                sendConnectEstablished(conn);
                break;
            case CLOSED:
            default:
                break;
        }
    }

    private void sendConnectResponse(Connection conn) {
        ConnectResponseMessage resp = new ConnectResponseMessage();
        byte[] respData = resp.serialize();
        send(conn.getAddress(), respData, resp.getDataLength());
    }

    private void handleConnectionResponse(Address addr, byte[] data, int dataIdx) {
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        switch (conn.getCtrlState()) {
            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
                conn.setCtrlState(ControlConnectionState.CONNECTED);
                connHolder.promoteConnection(conn);
                messageQueue.enqueue(new ConnectedMessage(conn));
                sendConnectEstablished(conn);
                break;
            case CONNECTED:
                Logger.info("Already connected to %s. Resend connect established.", conn);
                sendConnectEstablished(conn);
                break;
            case DISCONNECTED:
            case CLOSED:
            default:
                break;
        }
    }


    private void handleConnectionEstablished(Address addr, byte[] data, int dataIdx) {
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            conn = connHolder.createAsIncomingHandshake(addr);
        }

        switch (conn.getCtrlState()) {
            case AWAITING_CONNECT_RESPONSE:
            case AWAITING_CONNECT_ESTABLISHED:
            case SEND_CONNECT_REQUEST:
                conn.setCtrlState(ControlConnectionState.CONNECTED);
                connHolder.promoteConnection(conn);
                messageQueue.enqueue(new ConnectedMessage(conn));
                break;
            case CONNECTED:
                Logger.info("Already connected to %s.", conn);
                break;
            case DISCONNECTED:
            case CLOSED:
            default:
                break;
        }
    }

    private void handleUserData(Address addr, byte[] data, int dataIdx) {
        Connection conn = connHolder.getHandshakeOrConnection(addr.getId());
        if (conn == null) {
            Logger.warn("Received user data from %s but handshake nor connection doesn't even exist!.", addr);
            return;
        }

        switch (conn.getCtrlState()) {
            case CONNECTED:
                Logger.info("Received user data from %s.", conn);
                // enqueue user data.
                break;
            case AWAITING_CONNECT_ESTABLISHED:
                // received user data while waiting for connect established. Connect established message must got lost in
                // its way. Consider this user data as a sign of connection established.
                conn.setCtrlState(ControlConnectionState.CONNECTED);
                connHolder.promoteConnection(conn);
                messageQueue.enqueue(new ConnectedMessage(conn));
                // enqueue user data.
                break;
            case AWAITING_CONNECT_RESPONSE:
            case SEND_CONNECT_REQUEST:
            case DISCONNECTED:
                Logger.warn("Received user data from %s but isn't connected yet.", conn);
            case CLOSED:
                Logger.warn("Received user data from %s but connection is closed!.", addr);
            default:
                break;
        }
    }

    private void sendConnectEstablished(Connection conn) {
        ConnectEstablishedMessage resp = new ConnectEstablishedMessage();
        byte[] respData = resp.serialize();
        send(conn.getAddress(), respData, resp.getDataLength());
    }

    private void handleConnections() {
        connHolder.getConnections().forEach(Connection::handshake);
    }
}
