package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;
import com.rozsa.network.message.outgoing.MessageType;

public abstract class Message {
    protected final MessageType type;
    protected final DeliveryMethod method;
    protected byte[] data;
    protected int dataIdx = 0;

    public Message(MessageType type, DeliveryMethod method, int size, short seqNumber) {
        this.type = type;
        this.method = method;
        data = new byte[size];

        data[dataIdx++] = type.getId();
        data[dataIdx++] = method.getId();
        data[dataIdx++] = (byte)(seqNumber & 0xFF);
        data[dataIdx++] = (byte)((seqNumber >> 8) & 0xFF);
    }

    // maybe not public
    public void writeSeqNumber(short seqNumber) {
        data[1] = (byte)(seqNumber & 0xFF);
        data[2] = (byte)((seqNumber >> 8) & 0xFF);
    }

    public MessageType getType() {
        return type;
    }

    public byte[] getData() {
        return data;
    }

    public int getDataLength() {
        return dataIdx;
    }

    // TODO: add serialization methods. writeInt; writeString etc
}
