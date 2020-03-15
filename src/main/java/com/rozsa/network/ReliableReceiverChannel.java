package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

class ReliableReceiverChannel implements ReceiverChannel {
    private final Address addr;
    private final PacketSender sender;
    private final CachedMemory cachedMemory;
    private final Map<Short, IncomingMessage> withholdSeqNumbers;
    private final Set<Short> acksToSend;

    protected final DeliveryMethod type;
    protected final ConcurrentLinkedQueue<IncomingMessage> incomingMessages;
    protected final IncomingMessagesQueue incomingMessagesQueue;

    private short expectedSeqNumber;
    private short maxSeqNumber;
    private short windowSize;

    ReliableReceiverChannel(
            Address addr,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory,
            short maxSeqNumber,
            short windowSize
    ) {
        this.addr = addr;
        this.sender = sender;
        this.cachedMemory = cachedMemory;
        this.maxSeqNumber = maxSeqNumber;
        this.windowSize = windowSize;
        this.type = DeliveryMethod.RELIABLE;
        this.incomingMessagesQueue = incomingMessagesQueue;

        expectedSeqNumber = 0;
        acksToSend = new HashSet<>();
        withholdSeqNumbers = new HashMap<>();
        incomingMessages = new ConcurrentLinkedQueue<>();
    }

    public void enqueue(IncomingMessage msg) {
        incomingMessages.add(msg);
    }

    public void update() {
        acksToSend.clear();
        while (!incomingMessages.isEmpty()) {
            handleIncomingMessage(incomingMessages.poll());
        }

        sendAcks();
    }

    private void sendAcks() {
        if (acksToSend.isEmpty()) {
            return;
        }

        int payloadSize = (acksToSend.size() - 1) * 2 + NetConstants.MsgHeaderSize;
        byte[] buf = cachedMemory.allocBuffer(payloadSize);

        int bufIdx = 0;
        buf[bufIdx++] = MessageType.ACK.getId();
        buf[bufIdx++] = DeliveryMethod.RELIABLE.getId();

        Iterator<Short> acksIt = acksToSend.iterator();
        Short seqNumber = acksIt.next();
        buf[bufIdx++] = (byte)((seqNumber >> 8) & 0xFF);
        buf[bufIdx++] = (byte)(seqNumber & 0xFF);

        while (acksIt.hasNext()) {
            Short ack = acksIt.next();
            buf[bufIdx++] = (byte)((ack >> 8) & 0xFF);
            buf[bufIdx++] = (byte)(ack & 0xFF);
        }

        sender.send(addr, buf, bufIdx, true);
    }

    private void handleIncomingMessage(IncomingMessage message) {
        if (message.getType() != IncomingMessageType.USER_DATA) {
            cachedMemory.freeBuffer(message.getData());
            return;
        }

        short seqNumber = message.getSeqNumber();
        int relativeAckNumber = expectedSeqNumber - ((seqNumber < expectedSeqNumber) ? seqNumber + maxSeqNumber : seqNumber);

        if (relativeAckNumber == 0) {
            acksToSend.add(seqNumber);
            incomingMessagesQueue.enqueue(message);
            updateExpectedSeqNumber();
        }
        // ack is inside window
        else if (Math.abs(relativeAckNumber) < windowSize) {
            acksToSend.add(seqNumber);
            if (withholdSeqNumbers.containsKey(seqNumber)) {
                cachedMemory.freeBuffer(message.getData());
                return;
            }
            incomingMessagesQueue.enqueue(message);
            withholdSeqNumbers.put(seqNumber, message);
        }
        else {
            // We don't expect this message sequence number.. resend its ack
            acksToSend.add(seqNumber);
            cachedMemory.freeBuffer(message.getData());
        }
    }

    private void updateExpectedSeqNumber() {
        expectedSeqNumber = (short)((expectedSeqNumber + 1) % maxSeqNumber);
        Logger.debug("Updated expected SEQ number %d", expectedSeqNumber);
        while(withholdSeqNumbers.containsKey(expectedSeqNumber)) {
            withholdSeqNumbers.remove(expectedSeqNumber);
            expectedSeqNumber = (short)((expectedSeqNumber + 1) % maxSeqNumber);
            Logger.debug("Updated expected SEQ number %d", expectedSeqNumber);
        }
    }
}
