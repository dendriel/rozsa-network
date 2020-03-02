package com.rozsa.network.message.outgoing;

import com.rozsa.network.Message;

public class OutgoingMessage extends Message {
    public OutgoingMessage(MessageType type, short seqNumber) {
        super(type, 3, seqNumber);
    }

    public OutgoingMessage(MessageType type, int size, short seqNumber) {
        super(type, size+3, seqNumber);
    }
}
