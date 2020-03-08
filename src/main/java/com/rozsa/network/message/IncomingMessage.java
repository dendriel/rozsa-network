package com.rozsa.network.message;

import com.rozsa.network.Connection;

import java.util.Objects;

public class IncomingMessage {
    private final IncomingMessageType type;
    private final byte[] data;
    private final int length;
    private final short seqNumber;
    private Connection connection;

    IncomingMessage(IncomingMessageType type, Connection connection) {
        this(type, connection, (short)0, null, 0);
    }

    public IncomingMessage(IncomingMessageType type, Connection connection, short seqNumber, byte[] data, int length) {
        this.type = type;
        this.connection = connection;
        this.seqNumber = seqNumber;
        this.data = data;
        this.length = length;
    }

    public short getSeqNumber() {
        return seqNumber;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncomingMessage that = (IncomingMessage) o;
        return seqNumber == that.seqNumber &&
                type == that.type &&
                connection.equals(that.connection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, seqNumber, connection);
    }
}
