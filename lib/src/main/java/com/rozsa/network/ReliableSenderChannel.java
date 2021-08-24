package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.security.InvalidParameterException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
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

    private AtomicInteger lastFragmentationGroupId;
    private final int mtu;

    ReliableSenderChannel(
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            short windowSize,
            short maxSeqNumbers,
            int mtu,
            LongSupplier resendDelayProvider
    ) {
        this(address, sender, cachedMemory, windowSize, maxSeqNumbers, mtu, resendDelayProvider, DeliveryMethod.RELIABLE, 0);
    }

    ReliableSenderChannel(
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            short windowSize,
            short maxSeqNumbers,
            int mtu,
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
        this.mtu = mtu;
        this.resendDelayProvider = resendDelayProvider;
        this.cachedMemory = cachedMemory;

        lastFragmentationGroupId = new AtomicInteger();
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
        int msgLen = msg.getDataWritten();
        int userPayload = mtu - NetConstants.MsgHeaderSize;
        if (msgLen <= userPayload) {
            outgoingMessages.add(msg);
            return;
        }

        if (msgLen > NetConstants.MaxFragGroupLength) {
            throw new InvalidParameterException(String.format("Maximum allowed message length is %d bytes.", NetConstants.MaxFragGroupLength));
        }

        int fragGroup = lastFragmentationGroupId.getAndIncrement();

        byte[] dataToSend = msg.getData();
        int dataToSendLength = msg.getDataWritten();
        int maxFragSize = mtu - NetConstants.MsgTotalFragHeaderSize;
        int nextFragOffset = 0;
        int remainingBytesToFrag = msgLen;
        do {
            int chunkLength = Math.min(remainingBytesToFrag, maxFragSize);
            remainingBytesToFrag -= maxFragSize;
            OutgoingMessage chunk = new OutgoingMessage(cachedMemory, chunkLength, fragGroup, nextFragOffset, dataToSendLength);
//            Logger.error("Sent frag group %d - Offset: %d,  Len: %d, Remaining bytes: %d", fragGroup, nextFragOffset, chunkLength, remainingBytesToFrag);
            System.arraycopy(dataToSend, nextFragOffset, chunk.getData(), 0, chunkLength);
            chunk.incrementDataIdx(chunkLength);
            nextFragOffset += chunkLength;

            outgoingMessages.add(chunk);
        } while (remainingBytesToFrag > 0);

        cachedMemory.freeBuffer(msg.getData());
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

            boolean isFrag = msg.isFrag();
            int headerSize = isFrag ? NetConstants.MsgTotalFragHeaderSize : NetConstants.MsgHeaderSize;
            int bufSize = msg.getDataWritten() + headerSize;
            byte[] buf = cachedMemory.allocBuffer(bufSize);
            int bufIdx = 0;
            buf[bufIdx++] = channelId;
            buf[bufIdx++] = (byte)(((seqNumber << 1) & 0xFF) | (isFrag ? 1 : 0));
            buf[bufIdx++] = (byte)((seqNumber >> 7)& 0xFF);

            if (isFrag) {
                int fragGroup = msg.getFragGroup();
                buf[bufIdx++] = (byte)((fragGroup >> 8) & 0xFF);
                buf[bufIdx++] = (byte)(fragGroup & 0xFF);
                int fragGroupLength = msg.getFragGroupLength();
                buf[bufIdx++] = (byte)((fragGroupLength >> 8) & 0xFF);
                buf[bufIdx++] = (byte)(fragGroupLength & 0xFF);
                int fragOffset = msg.getFragOffset();
                buf[bufIdx++] = (byte)((fragOffset >> 8) & 0xFF);
                buf[bufIdx++] = (byte)(fragOffset & 0xFF);
            }

//            Logger.error("Send SEQ NUMBER %d", seqNumber);

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
