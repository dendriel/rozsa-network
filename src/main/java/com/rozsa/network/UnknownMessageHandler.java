package com.rozsa.network;

class UnknownMessageHandler implements IncomingMessageHandler {
    private final CachedMemory cachedMemory;

    UnknownMessageHandler(CachedMemory cachedMemory) {
        this.cachedMemory = cachedMemory;
    }

    @Override
    public void handle(Address addr, MessageType type, short seqNumber, byte[] data, int length, boolean isFrag) {
        cachedMemory.freeBuffer(data);
        Logger.error("Unknown message received from %s", addr);
    }
}
