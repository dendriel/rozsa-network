package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.LongSupplier;

class ReliableSenderChannel implements SenderChannel {
    protected final DeliveryMethod type;
    protected final byte channelId;
    protected final ConcurrentLinkedQueue<OutgoingMessage> outgoingMessages;
    protected final PacketSender sender;
    protected final Address addr;

    protected final StoredMessage[] storedMessages;
    protected final short windowSize;
    protected final short maxSeqNumbers;
    protected final CachedMemory cachedMemory;
    protected final Queue<IncomingMessage> incomingAcks;
    protected final boolean[] acks;
    protected final LongSupplier resendDelayProvider;

    protected short windowStart;
    protected short windowEnd;
    protected short seqNumber;

    ReliableSenderChannel(
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            short windowSize,
            short maxSeqNumbers,
            LongSupplier resendDelayProvider
    ) {
        this(address, sender, cachedMemory, windowSize, maxSeqNumbers, resendDelayProvider, DeliveryMethod.RELIABLE, 0);
    }

    ReliableSenderChannel(
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            short windowSize,
            short maxSeqNumbers,
            LongSupplier resendDelayProvider,
            DeliveryMethod type,
            int channelId
    ) {
        this.type = type;
        this.channelId = (byte)(type.getId() + channelId);
        this.addr = address;
        this.sender = sender;
        this.windowSize = windowSize;
        this.maxSeqNumbers = maxSeqNumbers;
        this.resendDelayProvider = resendDelayProvider;
        this.cachedMemory = cachedMemory;

        outgoingMessages = new ConcurrentLinkedQueue<>();
        storedMessages = new StoredMessage[this.windowSize];
        incomingAcks = new LinkedList<>();
        acks = new boolean[this.windowSize];
        windowStart = 0;
        windowEnd = 0;
        seqNumber = 0;
        initializeStoredMessages();
    }

    private void initializeStoredMessages() {
        for (int i = 0; i < storedMessages.length; i++) {
            storedMessages[i] = new StoredMessage();
        }
    }

    public void enqueue(OutgoingMessage msg) {
        outgoingMessages.add(msg);
    }

    public void enqueueAck(IncomingMessage ack) {
        incomingAcks.add(ack);
    }

    @Override
    public void update() {
        handleAcks();
        resendUnackedMessages();
        sendMessages();
    }

    private void handleAcks() {
        while (!incomingAcks.isEmpty()) {
            IncomingMessage ack = incomingAcks.poll();

            handleAck(ack.getSeqNumber());

            byte[] receivedAcks = ack.getData();

            // skip 1 byte from payload (this byte carries the channel type).
            for (int i = 0; i < (ack.getLength() / 2); i++) {
                int ackIdx = i * 2 + 1;
                short ackNumber = (short)((receivedAcks[ackIdx++] & 0xff) << 8);
                ackNumber = (short)(ackNumber | (receivedAcks[ackIdx] & 0xff));
                ackNumber = (short)(ackNumber % maxSeqNumbers);
                handleAck(ackNumber);
            }
            cachedMemory.freeBuffer(receivedAcks);
        }
    }

    protected void handleAck(short ackNumber) {
        int relativeAckNumber = windowStart - ((ackNumber < windowStart) ? ackNumber + maxSeqNumbers : ackNumber);
        int windowSlot = (short)(ackNumber % windowSize);
        // expected ack.
        if (relativeAckNumber == 0) {
            cachedMemory.freeBuffer(storedMessages[windowSlot].getEncodedMsg());
            storedMessages[windowSlot].reset();
            // clear received acks.
            do {
                acks[windowSlot] = false;
                windowStart = (short)((windowStart + 1) % maxSeqNumbers);
                windowSlot = (short)(windowStart % windowSize);
            } while (acks[windowSlot]);
        }
        // ack is inside window and is ahead of windowStart.
        else if (Math.abs(relativeAckNumber) < windowSize) {
            cachedMemory.freeBuffer(storedMessages[windowSlot].getEncodedMsg());
            storedMessages[windowSlot].reset();
            acks[windowSlot] = true;
        }
    }

    private void resendUnackedMessages() {
        long currTimeout = resendDelayProvider.getAsLong();
        for (int i = 0; i < windowSize; i++) {
            if (!storedMessages[i].isTimeout(currTimeout)) {
                continue;
            }
            sender.send(addr, storedMessages[i].getEncodedMsg(), storedMessages[i].getEncodedMsgLength(), false);
            storedMessages[i].resetSentTime();
        }
    }

    private void sendMessages() {
        while (!outgoingMessages.isEmpty() && Math.abs(1 + ((windowEnd < windowStart) ? (windowEnd + windowSize) : windowEnd) - windowStart) < windowSize) {
            OutgoingMessage msg = outgoingMessages.poll();

            int bufSize = msg.getDataWritten() + NetConstants.MsgHeaderSize;
            byte[] buf = cachedMemory.allocBuffer(bufSize);
            int bufIdx = 0;
            buf[bufIdx++] = channelId;
            buf[bufIdx++] = (byte)((seqNumber >> 8) & 0xFF);
            buf[bufIdx++] = (byte)(seqNumber & 0xFF);

            seqNumber = (short)((seqNumber + 1) % maxSeqNumbers);

            System.arraycopy(msg.getData(), 0, buf, bufIdx, msg.getDataWritten());
            cachedMemory.freeBuffer(msg.getData());

            short windowSlot = (short)(windowEnd % windowSize);
            windowEnd = (short)((windowEnd + 1) % maxSeqNumbers);
//            Logger.debug("Window Start %d; end updated to %d - prev slot was %d", windowStart, windowEnd, windowSlot);
            storedMessages[windowSlot].set(buf, bufSize);

            sender.send(addr, buf, bufSize, false);
        }
    }
}
