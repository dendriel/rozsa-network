package com.rozsa.network;

public class UnknownMessageHandler implements IncomingMessageHandler {
    @Override
    public void handle(Address addr, byte[] data, int length, short seqNumber) {
        Logger.error("Unknown message received from %s", addr);
    }
}
