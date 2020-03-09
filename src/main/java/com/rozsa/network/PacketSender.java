package com.rozsa.network;

public interface PacketSender {
    void send(Address address, byte[] data, int dataLen, boolean freeData);

    void sendProtocol(Address addr, MessageType type, DeliveryMethod method, short seqNumber);
}
