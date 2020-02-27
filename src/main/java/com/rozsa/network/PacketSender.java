package com.rozsa.network;

public interface PacketSender {
    void send(Address address, byte[] data, int dataLen);
}
