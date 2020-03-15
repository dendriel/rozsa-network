package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

class ReliableReceiverChannel implements ReceiverChannel {
    protected final Address addr;
    protected final PacketSender sender;
    protected final CachedMemory cachedMemory;
    protected final Map<Short, IncomingMessage> withholdSeqNumbers;
    protected final Set<Short> acksToSend;

    protected final DeliveryMethod type;
    protected final ConcurrentLinkedQueue<IncomingMessage> incomingMessages;
    protected final IncomingMessagesQueue incomingMessagesQueue;

    protected short expectedSeqNumber;
    protected short maxSeqNumber;
    protected short windowSize;

    ReliableReceiverChannel(
            Address addr,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory,
            short maxSeqNumber,
            short windowSize
    ) {
        this(addr, sender, incomingMessagesQueue, cachedMemory, maxSeqNumber, windowSize, DeliveryMethod.RELIABLE);
    }

    ReliableReceiverChannel(
            Address addr,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory,
            short maxSeqNumber,
            short windowSize,
            DeliveryMethod type
    ) {
        this.addr = addr;
        this.sender = sender;
        this.cachedMemory = cachedMemory;
        this.maxSeqNumber = maxSeqNumber;
        this.windowSize = windowSize;
        this.type = type;
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
        buf[bufIdx++] = type.getId();

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

    protected void handleIncomingMessage(IncomingMessage message) {
        if (message.getType() != IncomingMessageType.USER_DATA) {
            cachedMemory.freeBuffer(message.getData());
            return;
        }

        short seqNumber = message.getSeqNumber();
        int relativeAckNumber = expectedSeqNumber - ((seqNumber < expectedSeqNumber) ? seqNumber + maxSeqNumber : seqNumber);

        if (relativeAckNumber == 0) {
            Logger.debug("Right on time seq number: %d", seqNumber);
            acksToSend.add(seqNumber);
            incomingMessagesQueue.enqueue(message);
            updateExpectedSeqNumber();
        }
        // ack is inside window
        else if (Math.abs(relativeAckNumber) < windowSize) {
            acksToSend.add(seqNumber);
            if (withholdSeqNumbers.containsKey(seqNumber)) {
                Logger.debug("Already received seq number: %d - won't redispatch", seqNumber);
                cachedMemory.freeBuffer(message.getData());
                return;
            }
            Logger.debug("Dispatch message seq number: %d", seqNumber);
            incomingMessagesQueue.enqueue(message);
            withholdSeqNumbers.put(seqNumber, message);
        }
        else {
            Logger.debug("Unexpected seq number: %d - exp: %d - rel: %d", seqNumber, expectedSeqNumber, relativeAckNumber);
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
