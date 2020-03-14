package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

public class UnreliableSequencedReceiverChannel extends ReceiverChannel {
    private short expectedSequenceNumber;

    UnreliableSequencedReceiverChannel(IncomingMessagesQueue incomingMessagesQueue, CachedMemory cachedMemory) {
        super(DeliveryMethod.UNRELIABLE, incomingMessagesQueue, cachedMemory);
    }

    @Override
    protected void handleIncomingMessage(IncomingMessage message) {
        if (message.getType() != IncomingMessageType.USER_DATA) {
            cachedMemory.freeBuffer(message.getData());
            Logger.error("Unhandled channel message received: %s", message.getType());
            return;
        }

        incomingMessagesQueue.enqueue(message);
    }
}
