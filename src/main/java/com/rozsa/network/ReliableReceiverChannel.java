package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

class ReliableReceiverChannel implements ReceiverChannel {
    protected final Address addr;
    protected final PacketSender sender;
    protected final CachedMemory cachedMemory;
    protected final Set<Short> withholdSeqNumbers;
    protected final Set<Short> acksToSend;

    protected final DeliveryMethod type;
    protected final byte channelId;
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
        this(addr, sender, incomingMessagesQueue, cachedMemory, maxSeqNumber, windowSize, DeliveryMethod.RELIABLE,0);
    }

    ReliableReceiverChannel(
            Address addr,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory,
            short maxSeqNumber,
            short windowSize,
            DeliveryMethod type,
            int channelId
    ) {
        this.addr = addr;
        this.sender = sender;
        this.cachedMemory = cachedMemory;
        this.maxSeqNumber = maxSeqNumber;
        this.windowSize = windowSize;
        this.type = type;
        this.channelId = (byte)(type.getId() + channelId);
        this.incomingMessagesQueue = incomingMessagesQueue;

        expectedSeqNumber = 0;
        acksToSend = new HashSet<>();
        withholdSeqNumbers = new HashSet<>();
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

    /**
     * ACK header:
     *
     * 1 byte - message type.
     * 2 bytes - first ack (instead of seq number)
     * 1 byte = channel id.
     * ?*2 bytes = remaining acks.
     *
     */
    private void sendAcks() {
        if (acksToSend.isEmpty()) {
            return;
        }

        int payloadSize = NetConstants.MsgHeaderSize + 1 + (acksToSend.size() - 1) * 2;
        byte[] buf = cachedMemory.allocBuffer(payloadSize);

        int bufIdx = 0;
        buf[bufIdx++] = MessageType.ACK.getId();

        Iterator<Short> acksIt = acksToSend.iterator();
        Short seqNumber = acksIt.next();
        buf[bufIdx++] = (byte)((seqNumber >> 8) & 0xFF);
        buf[bufIdx++] = (byte)(seqNumber & 0xFF);

        buf[bufIdx++] = channelId;

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
            dispatch(message);
            updateExpectedSeqNumber();
        }
        // ack is inside window
        else if (Math.abs(relativeAckNumber) < windowSize) {
            acksToSend.add(seqNumber);
            if (withholdSeqNumbers.contains(seqNumber)) {
                Logger.debug("Already received seq number: %d - won't redispatch", seqNumber);
                cachedMemory.freeBuffer(message.getData());
                return;
            }
            Logger.debug("Dispatch message seq number: %d", seqNumber);
            dispatch(message);
            withholdSeqNumbers.add(seqNumber);
        }
        else {
            Logger.debug("Unexpected seq number: %d - exp: %d - rel: %d", seqNumber, expectedSeqNumber, relativeAckNumber);
            // We don't expect this message sequence number.. resend its ack
            acksToSend.add(seqNumber);
            cachedMemory.freeBuffer(message.getData());
        }
    }

    protected void dispatch(IncomingMessage message) {
        incomingMessagesQueue.enqueue(message);
    }

    private void updateExpectedSeqNumber() {
        expectedSeqNumber = (short)((expectedSeqNumber + 1) % maxSeqNumber);
        Logger.debug("Updated expected SEQ number %d", expectedSeqNumber);
        while(withholdSeqNumbers.contains(expectedSeqNumber)) {
            withholdSeqNumbers.remove(expectedSeqNumber);
            expectedSeqNumber = (short)((expectedSeqNumber + 1) % maxSeqNumber);
            Logger.debug("Updated expected SEQ number %d", expectedSeqNumber);
        }
    }
}
