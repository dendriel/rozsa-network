package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.util.HashMap;
import java.util.Map;

public class ReliableOrderedReceiverChannel extends ReliableReceiverChannel {
    private final Map<Short, IncomingMessage> withholdMessages;

    private short expectedSeqNumber;

    ReliableOrderedReceiverChannel(
            Address addr,
            PacketSender sender,
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory,
            short maxSeqNumber,
            short windowSize,
            int channelId
    ) {
        super(addr, sender, incomingMessagesQueue, cachedMemory, maxSeqNumber, windowSize, DeliveryMethod.RELIABLE_ORDERED, channelId);
        withholdMessages = new HashMap<>();
        expectedSeqNumber = 0;
    }

    @Override
    protected void dispatch(IncomingMessage message) {
        short seqNumber = message.getSeqNumber();
        if (seqNumber < expectedSeqNumber) {
            cachedMemory.freeBuffer(message.getData());
//            Logger.warn("Received an already dispatched message! Seq number: %d. Expected: %d", seqNumber, expectedSeqNumber);
            return;
        }
        else if (seqNumber > expectedSeqNumber) {
            withholdMessages.put(seqNumber, message);
            return;
        }

        do {
            super.dispatch(message);
            expectedSeqNumber = (short)((expectedSeqNumber + 1) % maxSeqNumber);
            message = withholdMessages.remove(expectedSeqNumber);
        } while(message != null);
    }
}
