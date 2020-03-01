package com.rozsa.network;

public enum ConnectionState {
    SEND_CONNECT_REQUEST,
    AWAITING_CONNECT_RESPONSE,
    AWAITING_CONNECT_ESTABLISHED,
    CONNECTED,
    DISCONNECTED,
    CLOSED,
}
