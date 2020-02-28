package com.rozsa.network.channel;

import com.rozsa.network.message.incoming.IncomingMessage;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ReceiverChannel extends BaseChannel {
    private final ConcurrentLinkedQueue<IncomingMessage> incomingMessages;

    ReceiverChannel(DeliveryMethod type) {
        super(type);

        incomingMessages = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void update() {

    }
}
