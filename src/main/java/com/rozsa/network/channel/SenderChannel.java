package com.rozsa.network.channel;

import com.rozsa.network.*;
import com.rozsa.network.message.OutgoingMessage;

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
            byte[] buf = MessageSerializer.serialize(MessageType.USER_DATA, type, (short)0, msg.getData(), msg.getDataLength());
            sender.send(addr, buf, buf.length);
        }
    }

    public static SenderChannel create(DeliveryMethod deliveryMethod, Address address, PacketSender sender) {
        switch (deliveryMethod) {
            case UNRELIABLE:
                return new UnreliableSenderChannel(address, sender);
            default:
                Logger.debug("Unhandled delivery method!! " + deliveryMethod);
                return new UnreliableSenderChannel(address, sender);
        }
    }
}
