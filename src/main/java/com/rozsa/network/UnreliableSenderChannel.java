package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

class UnreliableSenderChannel extends SenderChannel {
    UnreliableSenderChannel(Address addr, PacketSender sender, CachedMemory cachedMemory) {
        super(DeliveryMethod.UNRELIABLE, addr, sender, cachedMemory);
    }

    @Override
    public void enqueueAck(IncomingMessage ack) {}
}
