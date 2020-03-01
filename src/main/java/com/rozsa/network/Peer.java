package com.rozsa.network;


import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.message.incoming.IncomingMessage;
import com.rozsa.network.message.outgoing.OutgoingMessage;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Peer implements PacketSender, IncomingMessageQueue {
    private final ConnectionHolder connHolder;
    private final PeerConfig config;
    private PeerLoop peerLoop;

    private boolean isInitialized;

    private ConcurrentLinkedQueue<IncomingMessage> incomingMessages;

    public Peer(PeerConfig config) throws SocketException {
        connHolder = new ConnectionHolder(config,this);
        peerLoop = new PeerLoop(connHolder, this, config);
        this.config = config;
        isInitialized = false;
        incomingMessages = new ConcurrentLinkedQueue<>();
    }

    public void initialize() {
        peerLoop.start();
        isInitialized = true;
    }

    public IncomingMessage read() {
        return incomingMessages.poll();
    }

    // This must be private.
    public void enqueue(IncomingMessage message) {
        incomingMessages.add(message);
    }

    private void assertInitialized() throws NotActiveException {
        if (!isInitialized) {
            throw new NotActiveException("Peer is not initialized.");
        }
    }

    public Connection connect(String ip, int port) throws NotActiveException, UnknownHostException {
        assertInitialized();

        Address addr =  Address.from(ip, port);
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn != null) {
            Logger.info("Already connected to %s:%d", ip, port);
            return conn;
        }

        Logger.info("Connecting to %s:%d", ip, port);

        conn = connHolder.getHandshake(addr.getId());
        if (conn == null) {
            return connHolder.createAsOutgoingHandshake(addr);
        }

        conn.sendConnectRequest();
        return conn;
    }

    // This must be private.
    @Override
    public void send(Address address, byte[] data, int dataLen) {
        peerLoop.send(address, data, dataLen);
    }

    public void sendMessage(Connection conn, OutgoingMessage msg, DeliveryMethod deliveryMethod) {
        conn.enqueueMessage(msg, deliveryMethod);
    }
}
