package com.rozsa.network.channel;

import com.rozsa.network.Address;
import com.rozsa.network.message.outgoing.OutgoingMessage;
import com.rozsa.network.PacketSender;

import java.util.concurrent.ConcurrentLinkedQueue;

public class SenderChannel extends BaseChannel {
    private final ConcurrentLinkedQueue<OutgoingMessage> outgoingMessages;
    private final PacketSender sender;
    private final Address addr;

    SenderChannel(DeliveryMethod type, Address addr, PacketSender sender) {
        super(type);
        this.sender = sender;
        this.addr = addr;
        outgoingMessages = new ConcurrentLinkedQueue<>();
    }

    public void enqueue(OutgoingMessage msg) {
        outgoingMessages.add(msg);
    }

    @Override
    public void update() {
        while(!outgoingMessages.isEmpty()) {
            OutgoingMessage msg = outgoingMessages.poll();
            sender.send(addr, msg.serialize(), msg.getDataLength());
        }
    }
}
