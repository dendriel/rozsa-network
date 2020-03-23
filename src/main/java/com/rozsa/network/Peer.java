package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidParameterException;

import static com.rozsa.network.ConnectionState.*;
import static com.rozsa.network.DeliveryMethod.*;

public class Peer {
    private final PeerConfig config;
    private final ConnectionHolder connHolder;
    private final IncomingMessagesQueue incomingMessages;
    private final PeerLoop peerLoop;
    private final CachedMemory cachedMemory;

    private boolean isInitialized;

    public Peer(PeerConfig config) throws SocketException {
        this.config = config;

        incomingMessages = new PeerIncomingMessagesQueue();
        cachedMemory = new CachedMemory(config.getMaxCachedBufferCount());
        connHolder = new ConnectionHolder(config, incomingMessages, cachedMemory);
        peerLoop = new PeerLoop(connHolder, incomingMessages, cachedMemory, config, NetConstants.ReceiveMessagesThreshold);
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

    public int getMaxUserPayload() {
        return config.getMtu() - NetConstants.MsgHeaderSize;
    }

    public int getIncomingMessagesCount() {
        return incomingMessages.size();
    }

    public IncomingMessage read() {
        return incomingMessages.poll();
    }

    public OutgoingMessage createOutgoingMessage(int length) {
        return new OutgoingMessage(cachedMemory, length);
    }

    public void recycle(IncomingMessage msg) {
        cachedMemory.freeBuffer(msg.getData());
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

    public void sendMessage(Connection conn, OutgoingMessage msg, DeliveryMethod deliveryMethod, int channel) {
        if (channel >= NetConstants.MaxChannelsPerDeliveryMethod ||
            (channel > 0 &&
            (!deliveryMethod.equals(UNRELIABLE_SEQUENCED) &&
            !deliveryMethod.equals(RELIABLE_SEQUENCED) &&
            !deliveryMethod.equals(RELIABLE_ORDERED)))) {
            throw new InvalidParameterException(String.format("There are %d (0-%d) channels to be used with ordered and sequenced delivery methods. Invalid channel: %d",
                    NetConstants.MaxChannelsPerDeliveryMethod-1, NetConstants.MaxChannelsPerDeliveryMethod, channel));
        }

        conn.enqueueOutgoingMessage(msg, deliveryMethod, channel);
    }

    private void assertInitialized() throws NotActiveException {
        if (!isInitialized) {
            throw new NotActiveException("Peer is not initialized.");
        }
    }
}
