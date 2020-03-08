package com.rozsa.network;

@FunctionalInterface
public interface IncomingMessageHandler {
    void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length);
}
