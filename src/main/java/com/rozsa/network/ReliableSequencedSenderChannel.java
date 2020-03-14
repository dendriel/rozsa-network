package com.rozsa.network;

import java.util.function.LongSupplier;

public class ReliableSequencedSenderChannel extends ReliableSenderChannel {
    ReliableSequencedSenderChannel(
            Address address,
            PacketSender sender,
            CachedMemory cachedMemory,
            short windowSize,
            short maxSeqNumbers,
            LongSupplier resendDelayProvider
    ) {
        super(address, sender, cachedMemory, windowSize, maxSeqNumbers, resendDelayProvider, DeliveryMethod.RELIABLE_SEQUENCED);
    }

    @Override
    protected void handleAck(short ackNumber) {
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
        // A higher than expected ack number message was received by remote peer. So he won't need older messages.
        else if (ackNumber > windowStart) {
            cachedMemory.freeBuffer(storedMessages[ackNumber].getEncodedMsg());
            storedMessages[ackNumber].reset();
            acks[ackNumber] = true;
        }
    }
}
