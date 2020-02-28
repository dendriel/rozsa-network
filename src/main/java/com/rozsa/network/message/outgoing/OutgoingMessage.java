package com.rozsa.network.message.outgoing;

import com.rozsa.network.Message;
import com.rozsa.network.proto.MessageType;

public class OutgoingMessage extends Message {
    public OutgoingMessage(MessageType type) {
        super(type, 1);
    }

    public OutgoingMessage(MessageType type, int size) {
        super(type, size+1);
    }

    public byte[] serialize() {
        data[dataIdx++] = type.getId();
        return data;
    }
}
