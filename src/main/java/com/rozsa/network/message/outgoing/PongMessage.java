package com.rozsa.network.message.outgoing;

public class PongMessage extends OutgoingMessage {
    public PongMessage(short seqNumber) {
        super(MessageType.PONG, seqNumber);
    }
}
