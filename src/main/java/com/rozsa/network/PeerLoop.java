package com.rozsa.network;

import com.rozsa.network.message.incoming.ConnectedMessage;
import com.rozsa.network.proto.ConnectionResponseMessage;
import com.rozsa.network.proto.MessageType;

import java.net.DatagramPacket;
import java.net.SocketException;

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
        handleHandshakes();
        receiveIncomingPackets();
    }

    public void send(Address addr, byte[] data, int dataLen) {
        System.out.println("Sending outgoing message of " + MessageType.from(data[0]) + " " + addr);
        udpSocket.send(addr.getNetAddress(), addr.getPort(), data, dataLen);
    }

    private void handleHandshakes() {
        connHolder.getHandshakes().forEach(Connection::handshake);
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
        System.out.println("Received incoming message of " + type + " " + addr);

        switch (type) {
            case CONNECTION_REQUEST:
                handleConnectionRequest(addr, data, dataIdx);
                break;
            case CONNECTION_RESPONSE:
                handleConnectionResponse(addr, data, dataIdx);
                break;
            case CONNECTION_DENIED:
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
            case SEND_CONNECT_REQUEST:
            case AWAITING_CONNECT_RESPONSE:
                byte[] respData = new byte[1];
                ConnectionResponseMessage resp = new ConnectionResponseMessage();
                resp.serialize(respData, respData.length);
                send(addr, respData, respData.length);
                conn.setCtrlState(ControlConnectionState.CONNECTED);
                messageQueue.enqueue(new ConnectedMessage(addr, data, dataIdx));
                connHolder.promoteConnection(conn);
            case CONNECTED:
            case CLOSED:
            default:
                break;
        }
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
                messageQueue.enqueue(new ConnectedMessage(addr, data, dataIdx));
            case CONNECTED:
            case DISCONNECTED:
            case CLOSED:
            default:
                break;
        }
    }

    private void handleConnections() {
        connHolder.getConnections().forEach(Connection::handshake);
    }
}
