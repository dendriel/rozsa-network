package com.rozsa.network.message.outgoing;

import com.rozsa.network.Message;
import com.rozsa.network.channel.DeliveryMethod;

import static com.rozsa.network.channel.DeliveryMethod.UNRELIABLE;

public class OutgoingMessage extends Message {
    public OutgoingMessage(MessageType type, short seqNumber) {
        super(type, UNRELIABLE, 4, seqNumber);
    }

    public OutgoingMessage(MessageType type, DeliveryMethod method, int size, short seqNumber) {
        super(type, method, size+4, seqNumber);
    }
}
