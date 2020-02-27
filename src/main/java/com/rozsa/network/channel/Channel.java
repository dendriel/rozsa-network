package com.rozsa.network.channel;

public interface Channel {
    ChannelType getType();

    void update();
}
