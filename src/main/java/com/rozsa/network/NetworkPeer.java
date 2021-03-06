package com.rozsa.network;

import com.rozsa.network.message.*;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class NetworkPeer {
    private final Peer peer;
    private final Set<Consumer<ConnectionRequestMessage>> onConnectionRequestEventListeners;
    private final Set<Consumer<ConnectedMessage>> onConnectedEventListeners;
    private final Set<Consumer<DisconnectedMessage>> onDisconnectedEventListeners;
    private final Set<Consumer<PingUpdatedMessage>> onPingUpdatedEventListeners;

    public NetworkPeer(PeerConfig config) throws SocketException {
        peer = new Peer(config);
        onConnectionRequestEventListeners = new HashSet<>();
        onConnectedEventListeners = new HashSet<>();
        onDisconnectedEventListeners = new HashSet<>();
        onPingUpdatedEventListeners = new HashSet<>();
    }

    public void initialize() {
        peer.initialize();
    }

    public void terminate() {
        peer.terminate();
    }

    public int getIncomingMessagesCount() {
        return peer.getIncomingMessagesCount();
    }

    public void addOnConnectionRequestEventListeners(Consumer<ConnectionRequestMessage> listener) {
        onConnectionRequestEventListeners.add(listener);
    }

    public void removeOnConnectionRequestEventListeners(Consumer<ConnectionRequestMessage> listener) {
        onConnectionRequestEventListeners.remove(listener);
    }

    public void addOnConnectedEventListener(Consumer<ConnectedMessage> listener) {
        onConnectedEventListeners.add(listener);
    }

    public void removeOnConnectedEventListener(Consumer<ConnectedMessage> listener) {
        onConnectedEventListeners.remove(listener);
    }

    public void addOnDisconnectedEventListener(Consumer<DisconnectedMessage> listener) {
        onDisconnectedEventListeners.add(listener);
    }

    public void removeOnDisconnectedEventListener(Consumer<DisconnectedMessage> listener) {
        onDisconnectedEventListeners.remove(listener);
    }

    public void addOnPingUpdatedEventListener(Consumer<PingUpdatedMessage> listener) {
        onPingUpdatedEventListeners.add(listener);
    }

    public void removeOnPingUpdatedEventListener(Consumer<PingUpdatedMessage> listener) {
        onPingUpdatedEventListeners.remove(listener);
    }

    public Connection connect(String ip, int port, OutgoingMessage hailMessage) throws NotActiveException, UnknownHostException {
        return peer.connect(ip, port, hailMessage);
    }

    public void connect(String ip, int port) throws NotActiveException, UnknownHostException {
        peer.connect(ip, port);
    }

    public void disconnect(Connection conn) {
        peer.disconnect(conn);
    }

    public void approve(Connection conn) {
        peer.approve(conn);
    }

    public void deny(Connection conn) {
        peer.deny(conn, DisconnectReason.DENIED);
    }

    public void deny(Connection conn, DisconnectReason reason) {
        peer.deny(conn, reason);
    }

    public void sendMessage(Connection conn, OutgoingMessage msg, DeliveryMethod deliveryMethod) {
        sendMessage(conn, msg, deliveryMethod, 0);
    }

    public void sendMessage(Connection conn, OutgoingMessage msg, DeliveryMethod deliveryMethod, int channel) {
        peer.sendMessage(conn, msg, deliveryMethod, channel);
    }

    public void sendInternal(OutgoingMessage msg) {
        peer.sendInternal(msg);
    }

    public void sendInternal(Connection conn, OutgoingMessage msg) {
        peer.sendInternal(conn, msg);
    }


    public int getMaxUserPayload() {
        return peer.getMaxUserPayload();
    }

    public OutgoingMessage createOutgoingMessage(int length) {
        return peer.createOutgoingMessage(length);
    }

    public void recycle(IncomingMessage msg) {
        peer.recycle(msg);
    }

    public Collection<Connection> getConnections() {
        return peer.getConnections();
    }

    public Connection getConnection(long id) {
        return peer.getConnection(id);
    }

    public IncomingUserDataMessage read() {
        IncomingMessage msg = peer.read();
        if (msg == null) {
            return null;
        }

        switch (msg.getType()) {
            case CONNECTION_REQUEST:
                handleConnectionRequestMessage(msg);
                break;
            case CONNECTED:
                handleConnectedMessage(msg);
                break;
            case DISCONNECTED:
                handleDisconnectedMessage(msg);
                break;
            case PING_UPDATED:
                handlePingUpdatedMessage(msg);
                break;
            case USER_DATA:
                return (IncomingUserDataMessage)msg;
            default:
                break;
        }

        return null;
    }

    private void handleConnectionRequestMessage(IncomingMessage msg) {
        onConnectionRequestEventListeners.forEach(l -> l.accept((ConnectionRequestMessage) msg));
    }

    private void handleConnectedMessage(IncomingMessage msg) {
        onConnectedEventListeners.forEach(l -> l.accept((ConnectedMessage)msg));
    }

    private void handleDisconnectedMessage(IncomingMessage msg) {
        onDisconnectedEventListeners.forEach(l -> l.accept((DisconnectedMessage)msg));
    }

    private void handlePingUpdatedMessage(IncomingMessage msg) {
        onPingUpdatedEventListeners.forEach(l -> l.accept((PingUpdatedMessage)msg));
    }
}
