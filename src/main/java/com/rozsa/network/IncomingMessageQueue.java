package com.rozsa.network;

import com.rozsa.network.message.incoming.IncomingMessage;

public interface IncomingMessageQueue {
    void enqueue(IncomingMessage message);
}
