package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

class UnreliableSequencedReceiverChannel extends UnreliableReceiverChannel {
    private final short maxSeqNumber;
    private short expectedSeqNumber;

    UnreliableSequencedReceiverChannel(
            IncomingMessagesQueue incomingMessagesQueue,
            CachedMemory cachedMemory,
            short maxSeqNumber
    ) {
        super(incomingMessagesQueue, cachedMemory, DeliveryMethod.UNRELIABLE_SEQUENCED);
        this.maxSeqNumber = maxSeqNumber;
    }

    @Override
    protected void dispatch(IncomingMessage message) {
        short seqNumber = message.getSeqNumber();
        if (seqNumber < expectedSeqNumber) {
            cachedMemory.freeBuffer(message.getData());
            return;
        }

        incomingMessagesQueue.enqueue(message);
        expectedSeqNumber = (short)((seqNumber + 1) % maxSeqNumber);
    }
}
