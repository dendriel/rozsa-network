package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.OutgoingMessage;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.LongSupplier;


class ReliableSenderChannel extends SenderChannel {
    private final StoredMessage[] storedMessages;
    private final short windowSize;
    private final short maxSeqNumbers;
    private final CachedMemory cachedMemory;
    private final Queue<IncomingMessage> incomingAcks;
    private final boolean[] acks;
    private final LongSupplier resendDelayProvider;

    private short windowStart;
    private short windowEnd;
    private short seqNumber;

    ReliableSenderChannel(
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            short windowSize,
            short maxSeqNumbers,
            LongSupplier resendDelayProvider
    ) {
        super(DeliveryMethod.RELIABLE, address, sender, cachedMemory);
        this.windowSize = (short)(windowSize + 1);
        this.maxSeqNumbers = maxSeqNumbers;
        this.resendDelayProvider = resendDelayProvider;
        this.cachedMemory = cachedMemory;

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

            handleAck((short)(ack.getSeqNumber() % windowSize));

            byte[] receivedAcks = ack.getData();
            for (int i = 0; i < (ack.getLength() / 2); i++) {
                int ackIdx = i * 2;
                short ackNumber = (short)((receivedAcks[ackIdx++] & 0xff) << 8);
                ackNumber = (short)(ackNumber | (receivedAcks[ackIdx] & 0xff));
                ackNumber = (short)(ackNumber % windowSize);
                handleAck(ackNumber);
            }
            cachedMemory.freeBuffer(receivedAcks);
        }
    }

    private void handleAck(short ackNumber) {
        if (ackNumber == windowStart) {
            cachedMemory.freeBuffer(storedMessages[ackNumber].getEncodedMsg());
            storedMessages[ackNumber].reset();
            // clear received acks.
            do {
                acks[windowStart] = false;
                windowStart = (short)((windowStart + 1) % windowSize);
            } while (acks[windowStart]);
        }
        else if (windowStart <= (windowSize / 2) && ackNumber > (windowSize / 2)) {
            return;
        }
        else if (ackNumber > windowStart) {
            cachedMemory.freeBuffer(storedMessages[ackNumber].getEncodedMsg());
            storedMessages[ackNumber].reset();
            acks[ackNumber] = true;
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
        while (!outgoingMessages.isEmpty() && ((windowEnd + 1) % windowSize) != windowStart) {
            OutgoingMessage msg = outgoingMessages.poll();

            int bufSize = msg.getDataLength() + NetConstants.MsgHeaderSize;
            byte[] buf = cachedMemory.allocBuffer(bufSize);
            int bufIdx = 0;
            buf[bufIdx++] = MessageType.USER_DATA.getId();
            buf[bufIdx++] = type.getId();
            buf[bufIdx++] = (byte)((seqNumber >> 8) & 0xFF);
            buf[bufIdx++] = (byte)(seqNumber & 0xFF);

            seqNumber = (short)((seqNumber + 1) % maxSeqNumbers);

            System.arraycopy(msg.getData(), 0, buf, bufIdx, msg.getDataLength());
            cachedMemory.freeBuffer(msg.getData());

            short windowSlot = windowEnd;
            windowEnd = (short)((windowEnd + 1) % windowSize);
            storedMessages[windowSlot].set(buf, bufSize);

            sender.send(addr, buf, buf.length, false);
        }
    }
}
