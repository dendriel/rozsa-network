package com.rozsa.network;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

class ConnectionHolder {
    private final PeerConfig config;
    private final IncomingMessagesQueue incomingMessages;
    private final ConcurrentHashMap<Long, Connection> handshakes;
    private final ConcurrentHashMap<Long, Connection> connections;

    private PacketSender sender;

    ConnectionHolder(PeerConfig config, IncomingMessagesQueue incomingMessages) {
        this.config = config;
        this.incomingMessages = incomingMessages;
        handshakes = new ConcurrentHashMap<>();
        connections = new ConcurrentHashMap<>();
    }

    void setPacketSender(PacketSender sender) {
        this.sender = sender;
    }

    Connection getConnection(long id) {
        return connections.get(id);
    }

    Connection getHandshake(long id) {
        return connections.get(id);
    }

    Connection getHandshakeOrConnection(long id) {
        Connection conn = handshakes.get(id);
        if (conn == null) {
            return getConnection(id);
        }

        return conn;
    }

    void promoteConnection(Connection conn) {
        handshakes.remove(conn.getId());
        conn.setConnected();
        connections.put(conn.getId(), conn);
        Logger.debug("Handshake %s has been promoted.", conn.getAddress());
    }

    Connection createAsIncomingHandshake(Address addr) {
        return createAsHandshake(addr, ConnectionState.DISCONNECTED);
    }

    Connection createAsOutgoingHandshake(Address addr) {
        return createAsHandshake(addr, ConnectionState.SEND_CONNECT_REQUEST);
    }

    private Connection createAsHandshake(Address addr, ConnectionState state) {
        Connection handshake = new Connection(config, addr, sender, incomingMessages);
        handshake.setState(state);
        handshakes.put(addr.getId(), handshake);
        return handshake;
    }

    Collection<Connection> getConnections() {
        return connections.values();
    }

    Collection<Connection> getHandshakes() {
        return handshakes.values();
    }

    void removeHandshake(Connection conn) {
        handshakes.remove(conn.getId());
    }

    void removeConnection(Connection conn) {
        connections.remove(conn.getId());
    }
}
