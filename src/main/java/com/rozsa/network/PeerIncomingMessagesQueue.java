package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

class PeerIncomingMessagesQueue implements IncomingMessagesQueue {
    private final ConcurrentLinkedQueue<IncomingMessage> messages;

    PeerIncomingMessagesQueue() {
        messages = new ConcurrentLinkedQueue<>();
    }

    @Override
    public void enqueue(IncomingMessage message) {
        messages.add(message);
    }

    @Override
    public void enqueueAll(Collection<IncomingMessage> messages) {
        this.messages.addAll(messages);
    }

    @Override
    public IncomingMessage poll() {
        return messages.poll();
    }

    public int size() {
        return messages.size();
    }
}
