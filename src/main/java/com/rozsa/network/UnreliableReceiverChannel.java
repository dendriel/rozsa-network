package com.rozsa.network;

class UnreliableReceiverChannel extends ReceiverChannel {
    UnreliableReceiverChannel(IncomingMessagesQueue incomingMessagesQueue, CachedMemory cachedMemory) {
        super(DeliveryMethod.UNRELIABLE, incomingMessagesQueue, cachedMemory);
    }
}
