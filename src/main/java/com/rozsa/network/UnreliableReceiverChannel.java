package com.rozsa.network;

class UnreliableReceiverChannel extends ReceiverChannel {
    UnreliableReceiverChannel(IncomingMessagesQueue incomingMessagesQueue) {
        super(DeliveryMethod.UNRELIABLE, incomingMessagesQueue);
    }
}
