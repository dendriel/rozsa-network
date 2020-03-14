package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.util.concurrent.ConcurrentLinkedQueue;

class UnreliableSenderChannel implements SenderChannel {
    protected final DeliveryMethod type;
    protected final PacketSender sender;
    protected final Address addr;
    protected final CachedMemory cachedMemory;
    protected final ConcurrentLinkedQueue<OutgoingMessage> outgoingMessages;
    private final short maxSeqNumbers;

    private short seqNumber;

    UnreliableSenderChannel(Address addr, PacketSender sender, CachedMemory cachedMemory) {
        this(addr, sender, cachedMemory, DeliveryMethod.UNRELIABLE, (short)1);
    }

    UnreliableSenderChannel(Address addr, PacketSender sender, CachedMemory cachedMemory, DeliveryMethod type, short maxSeqNumbers) {
        this.sender = sender;
        this.addr = addr;
        this.cachedMemory = cachedMemory;
        this.type = type;
        this.maxSeqNumbers = maxSeqNumbers;
        outgoingMessages = new ConcurrentLinkedQueue<>();
    }

    public void enqueue(OutgoingMessage msg) {
        outgoingMessages.add(msg);
    }

    public void enqueueAck(IncomingMessage ack) {}

    public void update() {
        while(!outgoingMessages.isEmpty()) {
            OutgoingMessage msg = outgoingMessages.poll();

            int bufSize = msg.getDataWritten() + NetConstants.MsgHeaderSize;
            byte[] buf = cachedMemory.allocBuffer(bufSize);
            int bufIdx = 0;
            buf[bufIdx++] = MessageType.USER_DATA.getId();
            buf[bufIdx++] = type.getId();
            buf[bufIdx++] = (byte)((seqNumber >> 8) & 0xFF);
            buf[bufIdx++] = (byte)(seqNumber & 0xFF);

            // Seq number doesn't care for pure unreliable delivery method. It is used by unreliable sequenced method.
            seqNumber = (short)((seqNumber + 1) % maxSeqNumbers);

            System.arraycopy(msg.getData(), 0, buf, bufIdx, msg.getDataWritten());
            cachedMemory.freeBuffer(msg.getData());

            sender.send(addr, buf, bufSize, true);
        }
    }
}
