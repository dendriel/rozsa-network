package com.rozsa.network;

public interface IncomingMessageQueue {
    void enqueue(IncomingMessage message);
}
