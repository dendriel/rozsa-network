package com.rozsa.network.message;

import com.rozsa.network.Connection;
import com.rozsa.network.DeliveryMethod;
import com.rozsa.network.MessageType;
import com.rozsa.network.NetConstants;

import java.util.Objects;

public class IncomingMessage {
    private final IncomingMessageType type;
    protected final byte[] data;
    private final int length;
    private final short seqNumber;
    private final MessageType messageType;

    private Connection connection;

    private final boolean isFrag;
    private int fragGroup;
    private int fragGroupLength;
    private int fragOffset;
    private int fragGroupBytesWritten;

    IncomingMessage(IncomingMessageType type, Connection connection) {
        this(type, connection, (short)0, null, 0, MessageType.UNKNOWN, false);
    }

    public IncomingMessage(IncomingMessageType type, Connection connection, short seqNumber, byte[] data, int length, MessageType messageType, boolean isFrag) {
        this.type = type;
        this.connection = connection;
        this.seqNumber = seqNumber;
        this.data = data;
        this.length = length;
        this.messageType = messageType;
        this.isFrag = isFrag;

        if (isFrag) {
            int dataIdx = 0;
            fragGroup = ((data[dataIdx++] & 0xFF) << 8);
            fragGroup = (fragGroup | data[dataIdx++] & 0xFF);

            fragGroupLength = ((data[dataIdx++] & 0xFF) << 8);
            fragGroupLength = (fragGroupLength | data[dataIdx++] & 0xFF);

            fragOffset = ((data[dataIdx++] & 0xFF) << 8);
            fragOffset = (fragOffset | data[dataIdx] & 0xFF);
        }
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

    public MessageType getMessageType() {
        return messageType;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return length;
    }

    public int getFragGroup() {
        return fragGroup;
    }

    public int getFragGroupLength() {
        return fragGroupLength;
    }

    public boolean isFrag() {
        return isFrag;
    }

    public void combine(IncomingMessage frag) {
        int dataLength = frag.getLength() - NetConstants.MsgFragHeaderSize;
        System.arraycopy(frag.getData(), NetConstants.MsgFragHeaderSize, data, frag.fragOffset, dataLength);
        fragGroupBytesWritten += dataLength;

        fragGroup = frag.fragGroup;
        fragGroupLength = frag.fragGroupLength;
    }

    public boolean isComplete() {
        return fragGroupBytesWritten == fragGroupLength;
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
