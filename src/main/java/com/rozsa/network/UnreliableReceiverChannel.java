package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;
import com.rozsa.network.message.IncomingMessageType;

import java.util.concurrent.ConcurrentLinkedQueue;

class UnreliableReceiverChannel implements ReceiverChannel {
    protected final DeliveryMethod type;
    protected final ConcurrentLinkedQueue<IncomingMessage> incomingMessages;
    protected final IncomingMessagesQueue incomingMessagesQueue;
    protected final CachedMemory cachedMemory;

    UnreliableReceiverChannel(IncomingMessagesQueue incomingMessagesQueue, CachedMemory cachedMemory) {
        this(incomingMessagesQueue, cachedMemory, DeliveryMethod.UNRELIABLE);
    }

    UnreliableReceiverChannel(IncomingMessagesQueue incomingMessagesQueue, CachedMemory cachedMemory, DeliveryMethod type) {
        this.type = type;
        this.incomingMessagesQueue = incomingMessagesQueue;
        this.cachedMemory = cachedMemory;

        incomingMessages = new ConcurrentLinkedQueue<>();
    }

    public void enqueue(IncomingMessage msg) {
        incomingMessages.add(msg);
    }

    public void update() {
        IncomingMessage message;
        while (!incomingMessages.isEmpty()) {
            message = incomingMessages.poll();
            if (message.getType() != IncomingMessageType.USER_DATA) {
                cachedMemory.freeBuffer(message.getData());
                Logger.error("Unhandled channel message received: %s", message.getType());
                return;
            }

            dispatch(message);
        }
    }

    protected void dispatch(IncomingMessage message) {
        incomingMessagesQueue.enqueue(message);
    }
}
