package com.rozsa.network;


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
        connHolder = new ConnectionHolder(this);
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

        System.out.printf("Connecting to %s:%d\n", ip, port);

        Address address =  Address.from(ip, port);
        Connection conn = connHolder.getConnection(address.getId());
        if (conn == null) {
            return connHolder.createAsOutgoingHandshake(address);
        }

        return conn;
    }

    @Override
    public void send(Address address, byte[] data, int dataLen) {
        peerLoop.send(address, data, dataLen);
    }
}
