package com.rozsa.network;

public class ConnectionDeniedHandler implements IncomingMessageHandler {
    private final CachedMemory cachedMemory;

    public ConnectionDeniedHandler(CachedMemory cachedMemory) {
        this.cachedMemory = cachedMemory;
    }

    @Override
    public void handle(Address addr, MessageType type, short seqNumber, byte[] data, int length) {
        cachedMemory.freeBuffer(data);
    }
}
