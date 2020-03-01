package com.rozsa.network;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;


class ConnectionHolder {
    private final PeerConfig config;
    private final PacketSender sender;
    private final ConcurrentHashMap<Long, Connection> handshakes;
    private final ConcurrentHashMap<Long, Connection> connections;

    public ConnectionHolder(PeerConfig config, PacketSender sender) {
        this.config = config;
        this.sender = sender;
        handshakes = new ConcurrentHashMap<>();
        connections = new ConcurrentHashMap<>();
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
        conn.setCtrlState(ControlConnectionState.CONNECTED);
        connections.put(conn.getId(), conn);
        Logger.info("Handshake %s has been promoted.", conn.getAddress());
    }

    Connection createAsIncomingHandshake(Address addr) {
        return createAsHandshake(addr, ControlConnectionState.DISCONNECTED);
    }

    Connection createAsOutgoingHandshake(Address addr) {
        return createAsHandshake(addr, ControlConnectionState.SEND_CONNECT_REQUEST);
    }

    Connection createAsHandshake(Address addr, ControlConnectionState state) {
        Connection handshake = new Connection(config, addr, sender);
        handshake.setCtrlState(state);
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
}
