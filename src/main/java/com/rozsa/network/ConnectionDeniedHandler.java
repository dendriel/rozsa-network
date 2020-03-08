package com.rozsa.network;

public class ConnectionDeniedHandler implements IncomingMessageHandler {
    @Override
    public void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length) {

    }
}
