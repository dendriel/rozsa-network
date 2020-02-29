package com.rozsa.network;

public class PeerConfig {
    private int port;
    private int maximumHandshakeAttempts;
    private long intervalBetweenHandshakes;

    public PeerConfig() {
        this(0);
    }

    public PeerConfig(int port) {
        this.port = port;
        maximumHandshakeAttempts = Constants.DefaultMaximumHandshakeAttempts;
        intervalBetweenHandshakes = Constants.DefaultIntervalBetweenHandshakesInMillis;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaximumHandshakeAttempts() {
        return maximumHandshakeAttempts;
    }

    public void setMaximumHandshakeAttempts(int maximumHandshakeAttempts) {
        this.maximumHandshakeAttempts = maximumHandshakeAttempts;
    }

    public long getIntervalBetweenHandshakes() {
        return intervalBetweenHandshakes;
    }

    public void setIntervalBetweenHandshakes(long intervalBetweenHandshakes) {
        this.intervalBetweenHandshakes = intervalBetweenHandshakes;
    }
}
