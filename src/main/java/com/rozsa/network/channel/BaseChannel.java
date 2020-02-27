package com.rozsa.network.channel;

public class BaseChannel implements Channel {
    private final ChannelType type;

    BaseChannel(ChannelType type) {
        this.type = type;
    }

    @Override
    public ChannelType getType() {
        return type;
    }

    @Override
    public void update() {

    }
}
