package com.rozsa.network;

@FunctionalInterface
public interface IncomingMessageHandler {
    void handle(Address addr, MessageType type, short seqNumber, byte[] data, int length);
}
