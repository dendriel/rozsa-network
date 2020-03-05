package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;

@FunctionalInterface
public interface IncomingMessageHandler {
    void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length);
}
