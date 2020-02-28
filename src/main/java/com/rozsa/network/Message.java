package com.rozsa.network;

import com.rozsa.network.proto.MessageType;

public abstract class Message {
    protected final MessageType type;
    protected byte[] data;
    protected int dataIdx = 0;

    public Message(MessageType type, int size) {
        this.type = type;
        data = new byte[size];
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
