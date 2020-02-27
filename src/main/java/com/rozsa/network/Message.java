package com.rozsa.network;

public abstract class Message {
    protected byte[] data;

    protected int dataLen;

    public Message() {
    }

    public byte[] getData() {
        return data;
    }

    // TODO: add serialization methods. writeInt; writeString etc
}
