package com.rozsa.network;

import java.util.HashMap;

public enum MessageType {
    UNKNOWN(-1),
    CONNECTION_REQUEST(1),
    CONNECTION_RESPONSE(2),
    CONNECTION_ESTABLISHED(3),
    CONNECTION_DENIED(4),
    CONNECTION_CLOSED(5),
    PING(6),
    PONG(7),
    ACK(8),
    USER_DATA(9),
    ;

    private final byte id;

    MessageType(int id) {
        this.id = (byte)id;
    }

    byte getId() {
        return id;
    }

    private static HashMap<Byte, MessageType> idToTypeMapper;

    static {
        idToTypeMapper = new HashMap<>();
        idToTypeMapper.put(UNKNOWN.getId(), UNKNOWN);
        idToTypeMapper.put(CONNECTION_REQUEST.getId(), CONNECTION_REQUEST);
        idToTypeMapper.put(CONNECTION_RESPONSE.getId(), CONNECTION_RESPONSE);
        idToTypeMapper.put(CONNECTION_ESTABLISHED.getId(), CONNECTION_ESTABLISHED);
        idToTypeMapper.put(CONNECTION_DENIED.getId(), CONNECTION_DENIED);
        idToTypeMapper.put(CONNECTION_CLOSED.getId(), CONNECTION_CLOSED);
        idToTypeMapper.put(PING.getId(), PING);
        idToTypeMapper.put(PONG.getId(), PONG);
        idToTypeMapper.put(ACK.getId(), ACK);
        idToTypeMapper.put(USER_DATA.getId(), USER_DATA);
    }

    static MessageType from(byte id) {
        if (!idToTypeMapper.containsKey(id)) {
            return UNKNOWN;
        }

        return idToTypeMapper.get(id);
    }
}
