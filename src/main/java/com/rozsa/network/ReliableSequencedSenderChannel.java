package com.rozsa.network;

import java.util.function.LongSupplier;

class ReliableSequencedSenderChannel extends ReliableSenderChannel {
    ReliableSequencedSenderChannel(
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            short windowSize,
            short maxSeqNumbers,
            LongSupplier resendDelayProvider,
            int channelId
    ) {
        super(address, sender, cachedMemory, windowSize, maxSeqNumbers, resendDelayProvider, DeliveryMethod.RELIABLE_SEQUENCED, channelId);
    }

    @Override
    protected void handleAck(short ackNumber) {
        int relativeAckNumber = Math.abs(windowStart - ((ackNumber < windowStart) ? ackNumber + maxSeqNumbers : ackNumber));
//        Logger.debug("Received ack %d - relate %d", ackNumber, relativeAckNumber);
        if (relativeAckNumber < windowSize) {
            // An early message arrived. Discard all older messages.
            short windowSlot = (short)(windowStart % windowSize);
            int targetAckNumber = windowStart + relativeAckNumber;
            short counter = windowStart;
//            Logger.debug("Received valid ack %d - relative: %d - wStart %d - wTarget %d", ackNumber, relativeAckNumber, windowStart, targetAckNumber);
            do {
//                Logger.debug("Free ack %d - slot %d", windowStart, windowSlot);
                cachedMemory.freeBuffer(storedMessages[windowSlot].getEncodedMsg());
                storedMessages[windowSlot].reset();
                acks[windowSlot] = false;
                windowStart = (short)((windowStart + 1) % maxSeqNumbers);
                windowSlot = (short)(windowStart % windowSize);
                counter++;
            } while(counter <= targetAckNumber);
        }
    }
}
