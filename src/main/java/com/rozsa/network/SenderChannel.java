package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.LongSupplier;

abstract class SenderChannel {
    protected final DeliveryMethod type;
    protected final ConcurrentLinkedQueue<OutgoingMessage> outgoingMessages;
    protected final PacketSender sender;
    protected final Address addr;
    protected final CachedMemory cachedMemory;

    SenderChannel(DeliveryMethod type, Address addr, PacketSender sender, CachedMemory cachedMemory) {
        this.type = type;
        this.sender = sender;
        this.addr = addr;
        this.cachedMemory = cachedMemory;
        outgoingMessages = new ConcurrentLinkedQueue<>();
    }

    public DeliveryMethod getType() {
        return type;
    }

    public void enqueue(OutgoingMessage msg) {
        outgoingMessages.add(msg);
    }

    public abstract void enqueueAck(IncomingMessage ack);

    public void update() {
        while(!outgoingMessages.isEmpty()) {
            OutgoingMessage msg = outgoingMessages.poll();

            int bufSize = msg.getDataWritten() + NetConstants.MsgHeaderSize;
            byte[] buf = cachedMemory.allocBuffer(bufSize);
            int bufIdx = 0;
            buf[bufIdx++] = MessageType.USER_DATA.getId();
            buf[bufIdx++] = type.getId();
            buf[bufIdx++] = 0;
            buf[bufIdx++] = 0;

            System.arraycopy(msg.getData(), 0, buf, bufIdx, msg.getDataWritten());
            cachedMemory.freeBuffer(msg.getData());

            sender.send(addr, buf, bufSize, true);
        }
    }

    static SenderChannel create(
            DeliveryMethod deliveryMethod,
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            LongSupplier latencyProvider
    ) {
        switch (deliveryMethod) {
            case UNRELIABLE:
                return new UnreliableSenderChannel(address, sender, cachedMemory);
            case RELIABLE:
                return new ReliableSenderChannel(address, sender, cachedMemory, NetConstants.ReliableWindowSize, NetConstants.MaxSeqNumbers, latencyProvider);
            default:
                Logger.debug("Unhandled delivery method!! " + deliveryMethod);
                return new UnreliableSenderChannel(address, sender, cachedMemory);
        }
    }
}
