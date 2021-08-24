package com.rozsa.network;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

class ConnectionHolder {
    private final PeerConfig config;
    private final IncomingMessagesQueue incomingMessages;
    private final CachedMemory cachedMemory;
    private final ConcurrentHashMap<Long, Connection> handshakes;
    private final ConcurrentHashMap<Long, Connection> connections;

    private PacketSender sender;

    ConnectionHolder(PeerConfig config, IncomingMessagesQueue incomingMessages, CachedMemory cachedMemory) {
        this.config = config;
        this.incomingMessages = incomingMessages;
        this.cachedMemory = cachedMemory;
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
        return handshakes.get(id);
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
        return createAsHandshake(addr, ConnectionState.DISCONNECTED, null);
    }

    Connection createAsOutgoingHandshake(Address addr, OutgoingMessage hailMessage) {
        return createAsHandshake(addr, ConnectionState.SEND_CONNECT_REQUEST, hailMessage);
    }

    private Connection createAsHandshake(Address addr, ConnectionState state, OutgoingMessage hailMessage) {
        Connection handshake = new Connection(config, addr, sender, incomingMessages, cachedMemory);
        handshake.setState(state);
        handshake.setHailMessage(hailMessage);
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
