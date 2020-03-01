package com.rozsa.network;

/**
 * Reasons for a disconnection.
 */
public enum DisconnectReason {
    /**
     * Open.
     */
    NONE,
    /**
     * Closed by local peer.
     */
    LOCAL_CLOSE,
    /**
     * Closed by remote peer.
     */
    REMOTE_CLOSE,
    /**
     * Didn't receive ping inside expected time interval.
     */
    TIMEOUT,
    /**
     * Denied by remote peer.
     */
    DENIED,
    /**
     * Remote peer didn't responded to connection request.
     */
    NO_RESPONSE,
}
