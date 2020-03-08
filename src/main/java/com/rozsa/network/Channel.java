package com.rozsa.network;

interface Channel {
    DeliveryMethod getType();

    void update();
}
