package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

class UnreliableSenderChannel extends SenderChannel {
    UnreliableSenderChannel(Address addr, PacketSender sender) {
        super(DeliveryMethod.UNRELIABLE, addr, sender);
    }

    @Override
    public void enqueueAck(IncomingMessage ack) {}
}
