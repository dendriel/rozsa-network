package com.rozsa.network;

import com.rozsa.network.message.IncomingMessage;

import java.util.Collection;

/**
 * Holds messages released to be forwarded to the user.
 */
public interface IncomingMessagesQueue {
    /**
     * Enqueue a message to be forwarded to the user.
     * @param message the message to be enqueued.
     */
    void enqueue(IncomingMessage message);

    /**
     * Enqueue a collection of messages.
     * @param messages messages to be enqueued.
     */
    void enqueueAll(Collection<IncomingMessage> messages);

    /**
     * Read an incoming message if available.
     * @return if available, an IncomingMessage that may be casted according to its type.
     */
    IncomingMessage poll();

    /**
     * Count of incoming messages ready to read.
     * @return Count of incoming messages.
     */
    int size();
}
