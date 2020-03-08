package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.OutgoingMessage;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

abstract class SenderChannel extends BaseChannel {
    protected final ConcurrentLinkedQueue<OutgoingMessage> outgoingMessages;
    protected final PacketSender sender;
    protected final Address addr;

    SenderChannel(DeliveryMethod type, Address addr, PacketSender sender) {
        super(type);
        this.sender = sender;
        this.addr = addr;
        outgoingMessages = new ConcurrentLinkedQueue<>();
    }

    public void enqueue(OutgoingMessage msg) {
        outgoingMessages.add(msg);
    }

    public abstract void enqueueAck(IncomingMessage ack);

    @Override
    public void update() {
        while(!outgoingMessages.isEmpty()) {
            OutgoingMessage msg = outgoingMessages.poll();
            byte[] buf = MessageSerializer.serialize(MessageType.USER_DATA, type, (short)0, msg.getData(), msg.getDataLength());
            sender.send(addr, buf, buf.length);
        }
    }

    static SenderChannel create(DeliveryMethod deliveryMethod, Address address, PacketSender sender, Supplier<Long> latencyProvider) {
        switch (deliveryMethod) {
            case UNRELIABLE:
                return new UnreliableSenderChannel(address, sender);
            case RELIABLE:
                return new ReliableSenderChannel(address, sender, NetConstants.ReliableWindowSize, NetConstants.MaxSeqNumbers, latencyProvider);
            default:
                Logger.debug("Unhandled delivery method!! " + deliveryMethod);
                return new UnreliableSenderChannel(address, sender);
        }
    }
}
