package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

class UnreliableSequencedSenderChannel extends UnreliableSenderChannel {
    private short seqNumber;

    UnreliableSequencedSenderChannel(Address addr, PacketSender sender, CachedMemory cachedMemory) {
        super(addr, sender, cachedMemory, DeliveryMethod.UNRELIABLE_SEQUENCED);

        seqNumber = 0;
    }

    @Override
    public void enqueueAck(IncomingMessage ack) {
    }
}
