package com.rozsa.network;

import com.rozsa.network.proto.MessageType;

public class OutgoingMessage extends Message {
    private final MessageType type;

    protected int dataIdx = 0;

    public OutgoingMessage(MessageType type) {
        this.type = type;
    }

    public void serialize(byte[] data, int dataLen) {
        assert dataLen > 1 : "Required at least dataLen > 1 to serialize protocol message";

        data[dataIdx++] = type.getId();
    }
}
