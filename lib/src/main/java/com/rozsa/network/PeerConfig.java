package com.rozsa.network;

//TODO: add interface. Create a builder.
public class PeerConfig {
    /**
     * UDP port to connect to (client) or listen to (server).
     */
    private int port;

    /**
     * Maximum handshakes attempts while trying to connect to a peer.
     */
    private int maximumHandshakeAttempts;

    /**
     * Interval in milliseconds before handshake retry.
     */
    private long intervalBetweenHandshakes;

    /**
     * Maximum size of UDP data receiving buffer.
     */
    private int receiveBufferSize;

    /**
     * Ping (heartbeat) interval. Also used to calculate latency.
     */
    private float pingInterval;

    /**
     * Maximu waiting time to consider connection lost after not receiving any pings from connected peer.
     */
    private float connectionTimeout;

    /**
     * Enable connection latency report when updated.
     */
    private boolean isPingUpdatedEventEnabled;

    /**
     * Maximum number of byte[] to keep in cache. (if maximum is reached, some random entry will be removed to make
     * room to new cache entry).
     */
    private int maxCachedBufferCount;

    /**
     * Maximum transfer unit (UDP packet) without couting the RUDP header. If you set this wrong, some user may have
     * connection issues due to internet providers dropping packets. Also, use getMaxUserPayload() to subtract the RUDP
     * header size.
     */
    private int mtu;

    /**
     * Enable connection approval events.
     */
    private boolean connectionApprovalRequired;

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

        connectionApprovalRequired = NetConstants.DefaultConnectionApprovalRequired;
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

    public boolean isConnectionApprovalRequired() {
        return connectionApprovalRequired;
    }

    public void setConnectionApprovalRequired(boolean connectionApprovalRequired) {
        this.connectionApprovalRequired = connectionApprovalRequired;
    }
}
