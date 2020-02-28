package com.rozsa.network.channel;

public interface Channel {
    DeliveryMethod getType();

    void update();
}
