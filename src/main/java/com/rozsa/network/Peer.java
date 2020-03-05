package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.OutgoingMessage;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;

import static com.rozsa.network.ConnectionState.*;

public class Peer {
    private final PeerConfig config;
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue incomingMessages;
    private final PeerLoop peerLoop;

    private boolean isInitialized;

    public Peer(PeerConfig config) throws SocketException {
        this.config = config;

        incomingMessages = new PeerIncomingMessagesQueue();
        connHolder = new ConnectionHolder(config, incomingMessages);
        peerLoop = new PeerLoop(connHolder, incomingMessages, config);
        connHolder.setPacketSender(peerLoop);

        isInitialized = false;
    }

    public void initialize() {
        if (isInitialized) {
            return;
        }
        peerLoop.start();
        isInitialized = true;
    }

    public int getIncomingMessagesCount() {
        return incomingMessages.size();
    }

    public IncomingMessage read() {
        return incomingMessages.poll();
    }

    public Connection connect(String ip, int port) throws NotActiveException, UnknownHostException {
        assertInitialized();

        Address addr =  Address.from(ip, port);
        Connection conn = connHolder.getConnection(addr.getId());
        if (conn != null) {
            if (conn.getState() == CONNECTED) {
                Logger.debug("Already connected to %s:%d", ip, port);
                return conn;
            }
            // remove conn so we can start fresh from handshake.
            connHolder.removeConnection(conn);
        }

        Logger.info("Connecting to %s:%d", ip, port);

        conn = connHolder.getHandshake(addr.getId());
        if (conn == null) {
            return connHolder.createAsOutgoingHandshake(addr);
        }

        conn.sendConnectRequest();
        return conn;
    }

    public void disconnect(Connection conn) {
        conn.setDisconnectReason(DisconnectReason.LOCAL_CLOSE);
        conn.setState(DISCONNECTED);
    }

    public void sendMessage(Connection conn, OutgoingMessage msg, DeliveryMethod deliveryMethod) {
        conn.enqueueOutgoingMessage(msg, deliveryMethod);
    }

    private void assertInitialized() throws NotActiveException {
        if (!isInitialized) {
            throw new NotActiveException("Peer is not initialized.");
        }
    }
}
