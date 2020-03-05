package com.rozsa.network;

import com.rozsa.network.channel.DeliveryMethod;

public class ConnectionDeniedHandler implements IncomingMessageHandler {
    @Override
    public void handle(Address addr, DeliveryMethod deliveryMethod, short seqNumber, byte[] data, int length) {

    }
}
