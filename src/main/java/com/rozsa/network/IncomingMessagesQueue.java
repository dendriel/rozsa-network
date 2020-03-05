package com.rozsa.network;

import com.rozsa.network.message.incoming.IncomingMessage;

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
