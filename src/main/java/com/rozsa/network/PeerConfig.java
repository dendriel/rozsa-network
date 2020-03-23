package com.rozsa.network;

//TODO: add interface. Create a builder.
public class PeerConfig {
    private int port;
    private int maximumHandshakeAttempts;
    private long intervalBetweenHandshakes;
    private int receiveBufferSize;
    private float pingInterval;
    private float connectionTimeout;
    private boolean isPingUpdatedEventEnabled;
    private int maxCachedBufferCount;
    private int mtu;

    public PeerConfig() {
        this(0);
    }

    public PeerConfig(int port) {
        this.port = port;
        maximumHandshakeAttempts = NetConstants.DefaultMaximumHandshakeAttempts;
        intervalBetweenHandshakes = NetConstants.DefaultIntervalBetweenHandshakesInMillis;

        pingInterval = NetConstants.DefaultPingInterval;
        connectionTimeout = NetConstants.DefaultConnectionTimeout;
        isPingUpdatedEventEnabled = NetConstants.DefaultIsPingUpdatedEventEnabled;

        receiveBufferSize = NetConstants.DefaultReceiveBufferSize;

        maxCachedBufferCount = NetConstants.DefaultMaxCachedBufferCount;
        mtu = NetConstants.DefaultMTU;
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

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public float getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(float pingInterval) {
        this.pingInterval = pingInterval;
    }

    public float getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(float connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public boolean isPingUpdatedEventEnabled() {
        return isPingUpdatedEventEnabled;
    }

    public void setPingUpdatedEventEnabled(boolean pingUpdatedEventEnabled) {
        this.isPingUpdatedEventEnabled = pingUpdatedEventEnabled;
    }

    public int getMaxCachedBufferCount() {
        return maxCachedBufferCount;
    }

    public void setMaxCachedBufferCount(int maxCachedBufferCount) {
        this.maxCachedBufferCount = maxCachedBufferCount;
    }

    public int getMtu() {
        return mtu;
    }

    public void setMtu(int mtu) {
        this.mtu = mtu;
    }
}
