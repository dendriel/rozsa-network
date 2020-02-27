package com.rozsa.network;

import com.rozsa.network.message.incoming.IncomingMessageType;

public class IncomingMessage extends Message {
    private final IncomingMessageType type;
    private Address address;

    public IncomingMessage(IncomingMessageType type, Address address, byte[] data, int dataLen) {
        this.type = type;
        this.address = address;
        this.data = data;
        this.dataLen = dataLen;
    }

    public Address getAddress() {
        return address;
    }

    public IncomingMessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "IncomingMessage{" +
                "type=" + type +
                ", address=" + address +
                '}';
    }
}
