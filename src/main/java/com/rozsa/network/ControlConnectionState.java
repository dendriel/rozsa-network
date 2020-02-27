package com.rozsa.network;

public enum ControlConnectionState {
    SEND_CONNECT_REQUEST,
    AWAITING_CONNECT_RESPONSE,
    CONNECTED,
    DISCONNECTED,
    CLOSED,
}
