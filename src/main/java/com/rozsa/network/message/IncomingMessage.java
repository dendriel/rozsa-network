package com.rozsa.network.message;

import com.rozsa.network.Connection;
import com.rozsa.network.DeliveryMethod;
import com.rozsa.network.MessageType;

import java.util.Objects;

public class IncomingMessage {
    private final IncomingMessageType type;
    private final byte[] data;
    private final int length;
    private final short seqNumber;
    private final MessageType messageType;
    private Connection connection;

    IncomingMessage(IncomingMessageType type, Connection connection) {
        this(type, connection, (short)0, null, 0, MessageType.UNKNOWN);
    }

    public IncomingMessage(IncomingMessageType type, Connection connection, short seqNumber, byte[] data, int length, MessageType messageType) {
        this.type = type;
        this.connection = connection;
        this.seqNumber = seqNumber;
        this.data = data;
        this.length = length;
        this.messageType = messageType;
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

    public DeliveryMethod getDeliveryMethod() {
        return DeliveryMethod.from(messageType.getBaseId());
    }

    public int getChannel() {
        return messageType.getOffset();
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
