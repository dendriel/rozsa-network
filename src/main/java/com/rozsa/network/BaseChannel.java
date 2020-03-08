package com.rozsa.network;

abstract class BaseChannel implements Channel {
    final DeliveryMethod type;

    BaseChannel(DeliveryMethod type) {
        this.type = type;
    }

    @Override
    public DeliveryMethod getType() {
        return type;
    }
}
