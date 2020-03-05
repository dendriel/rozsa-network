package com.rozsa.network.message;

import com.rozsa.network.Connection;

public class IncomingMessage {
    private final IncomingMessageType type;
    private final byte[] data;
    private final int length;
    private Connection connection;

    IncomingMessage(IncomingMessageType type, Connection connection) {
        this(type, connection, null, 0);
    }

    IncomingMessage(IncomingMessageType type, Connection connection, byte[] data, int length) {
        this.type = type;
        this.connection = connection;
        this.data = data;
        this.length = length;
    }

    public Connection getConnection() {
        return connection;
    }

    public IncomingMessageType getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return length;
    }

    @Override
    public String toString() {
        return "IncomingMessage{" +
                "type=" + type +
                ", address=" + connection.getAddress() +
                '}';
    }
}
