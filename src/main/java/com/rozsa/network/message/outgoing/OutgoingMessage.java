package com.rozsa.network.message.outgoing;

import com.rozsa.network.Message;

public class OutgoingMessage extends Message {
    public OutgoingMessage(MessageType type) {
        super(type, 1);
    }

    public OutgoingMessage(MessageType type, int size) {
        super(type, size+1);
    }
}
