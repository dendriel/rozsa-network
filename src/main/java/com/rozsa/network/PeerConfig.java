package com.rozsa.network;

public class PeerConfig {
    private int port;

    public PeerConfig(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
