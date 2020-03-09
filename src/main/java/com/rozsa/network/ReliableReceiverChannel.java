package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

import java.util.*;

class ReliableReceiverChannel extends ReceiverChannel {
    private final Address addr;
    private final PacketSender sender;
    private final CachedMemory cachedMemory;
    private final Map<Short, IncomingMessage> withholdSeqNumbers;
    private final Set<Short> acksToSend;

    private short expectedSeqNumber;
    private short maxSeqNumber;

    ReliableReceiverChannel(
            Address addr,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory,
            short maxSeqNumber) {
        super(DeliveryMethod.RELIABLE, incomingMessagesQueue);
        this.addr = addr;
        this.sender = sender;
        this.cachedMemory = cachedMemory;
        this.maxSeqNumber = maxSeqNumber;

        expectedSeqNumber = 0;
        acksToSend = new HashSet<>();
        withholdSeqNumbers = new HashMap<>();
    }

    @Override
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

        sender.send(addr, buf, bufIdx);
    }

    private void handleIncomingMessage(IncomingMessage message) {
        if (message.getType() != IncomingMessageType.USER_DATA) {
//            Logger.error("Unhandled channel message received: %s", message.getType());
            return;
        }

        short seqNumber = message.getSeqNumber();
        if (seqNumber == expectedSeqNumber) {
//            Logger.debug("Received expected msg seq number %d", expectedSeqNumber);
            acksToSend.add(expectedSeqNumber);
            incomingMessagesQueue.enqueue(message);
            updateExpectedSeqNumber();
        }
        else if (expectedSeqNumber <= (maxSeqNumber / 2) && seqNumber > (maxSeqNumber / 2)) {
            // if received seq number is not in the same half as the expected seq number, probably
            // received a already acked seq number.
//            Logger.debug("Unexpected message. Different half: %d - exp %d", seqNumber, expectedSeqNumber);
            acksToSend.add(seqNumber);
        }
        // too early message?
        else if (seqNumber > expectedSeqNumber) {
//            Logger.debug("Received to early msg seq number %d expected %d", seqNumber, expectedSeqNumber);
            if (withholdSeqNumbers.containsKey(seqNumber)) {
//                Logger.debug("Already dispatched this message seq number %d", seqNumber);
                return;
            }
            acksToSend.add(seqNumber);
            incomingMessagesQueue.enqueue(message);
            withholdSeqNumbers.put(seqNumber, message);
        }
        else {
            // We don't expect this message sequence number.. resend its ack
//            Logger.debug("Unexpected message: %d - exp %d", seqNumber, expectedSeqNumber);
            acksToSend.add(seqNumber);
        }
    }

    private void updateExpectedSeqNumber() {
        expectedSeqNumber = (short)((expectedSeqNumber + 1) % maxSeqNumber);
//        Logger.debug("Updated expected SEQ number %d", expectedSeqNumber);
        while(withholdSeqNumbers.containsKey(expectedSeqNumber)) {
            withholdSeqNumbers.remove(expectedSeqNumber);
            expectedSeqNumber = (short)((expectedSeqNumber + 1) % maxSeqNumber);
//            Logger.debug("Updated expected SEQ number %d", expectedSeqNumber);
        }
    }
}
