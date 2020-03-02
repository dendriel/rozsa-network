package com.rozsa.network;

@FunctionalInterface
public interface IncomingMessageHandler {
    void handle(Address addr, byte[] data, int length, short seqNumber);
}
