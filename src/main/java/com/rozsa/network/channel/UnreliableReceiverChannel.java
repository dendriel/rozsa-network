package com.rozsa.network.channel;

import com.rozsa.network.IncomingMessagesQueue;

class UnreliableReceiverChannel extends ReceiverChannel {
    UnreliableReceiverChannel(IncomingMessagesQueue incomingMessagesQueue) {
        super(DeliveryMethod.UNRELIABLE, incomingMessagesQueue);
    }
}
