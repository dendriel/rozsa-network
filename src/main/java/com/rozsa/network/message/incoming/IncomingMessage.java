package com.rozsa.network.message.incoming;

import com.rozsa.network.Connection;

public class IncomingMessage {
    private final IncomingMessageType type;
    private final byte[] data;
    private final int dataLen;
    private Connection connection;

    IncomingMessage(IncomingMessageType type, Connection connection, byte[] data, int dataLen) {
        this.type = type;
        this.connection = connection;
        this.data = data;
        this.dataLen = dataLen;
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

    public int getDataLen() {
        return dataLen;
    }

    @Override
    public String toString() {
        return "IncomingMessage{" +
                "type=" + type +
                ", address=" + connection.getAddress() +
                '}';
    }
}
