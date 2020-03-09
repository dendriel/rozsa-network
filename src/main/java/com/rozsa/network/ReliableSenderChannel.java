package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.OutgoingMessage;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;


class ReliableSenderChannel extends SenderChannel {
    private final StoredMessage[] storedMessages;
    private final short windowSize;
    private final short maxSeqNumbers;
    private final Queue<IncomingMessage> incomingAcks;
    private final boolean[] acks;
    private final Supplier<Long> resendDelayProvider;

    private short windowStart;
    private short windowEnd;
    private short seqNumber;

    ReliableSenderChannel(Address address, PacketSender sender, short windowSize, short maxSeqNumbers, Supplier<Long> resendDelayProvider) {
        super(DeliveryMethod.RELIABLE, address, sender);
        this.windowSize = (short)(windowSize + 1);
        this.maxSeqNumbers = maxSeqNumbers;
        this.resendDelayProvider = resendDelayProvider;

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
//        Logger.debug("enqueue ack %d", ack.getSeqNumber());
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
                short ackNumber = (short)(receivedAcks[ackIdx++] & 0xff);
                ackNumber = (short)((ackNumber << 8) | (receivedAcks[ackIdx] & 0xff));
                ackNumber = (short)(ackNumber % windowSize);
                handleAck(ackNumber);
            }
        }
    }

    private void handleAck(short ackNumber) {
        if (ackNumber == windowStart) {
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
            storedMessages[ackNumber].reset();
            acks[ackNumber] = true;
        }
    }

    private void resendUnackedMessages() {
        long currTimeout = resendDelayProvider.get();
        for (int i = 0; i < windowSize; i++) {
            if (!storedMessages[i].isTimeout(currTimeout)) {
                continue;
            }
            OutgoingMessage msg = storedMessages[i].getMessage();
            byte[] buf = MessageSerializer.serialize(MessageType.USER_DATA, type, storedMessages[i].getSeqNumber(), msg.getData(), msg.getDataLength());
            storedMessages[i].resetSentTime();
            sender.send(addr, buf, buf.length);
//            Logger.debug("Resent unacked message seq %d", storedMessages[i].getSeqNumber());
        }
    }

    private boolean canSend() {
        return ((windowEnd + 1) % windowSize) != windowStart;
    }

    private void sendMessages() {
        while (!outgoingMessages.isEmpty() && canSend()) {
            OutgoingMessage msg = outgoingMessages.poll();
            short msgSeqNumber = seqNumber;
            seqNumber = (short)((seqNumber + 1) % maxSeqNumbers);
            byte[] buf = MessageSerializer.serialize(MessageType.USER_DATA, type, msgSeqNumber, msg.getData(), msg.getDataLength());
            sender.send(addr, buf, buf.length);

            short windowSlot = windowEnd;
            windowEnd = (short)((windowEnd + 1) % windowSize);
            storedMessages[windowSlot].set(msg, msgSeqNumber);
        }
    }
}