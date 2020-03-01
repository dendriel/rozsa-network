package com.rozsa.network.message.outgoing;

import java.util.HashMap;

public enum MessageType {
    UNKNOWN(-1),
    CONNECT_REQUEST(1),
    CONNECT_RESPONSE(2),
    CONNECT_ESTABLISHED(3),
    CONNECT_DENIED(4),
    USER_DATA(5),
    ;

    private final byte id;

    MessageType(int id) {
        this.id = (byte)id;
    }

    public byte getId() {
        return id;
    }

    private static HashMap<Byte, MessageType> idToTypeMapper;

    static {
        idToTypeMapper = new HashMap<>();
        idToTypeMapper.put(UNKNOWN.getId(), UNKNOWN);
        idToTypeMapper.put(CONNECT_REQUEST.getId(), CONNECT_REQUEST);
        idToTypeMapper.put(CONNECT_RESPONSE.getId(), CONNECT_RESPONSE);
        idToTypeMapper.put(CONNECT_ESTABLISHED.getId(), CONNECT_ESTABLISHED);
        idToTypeMapper.put(CONNECT_DENIED.getId(), CONNECT_DENIED);
        idToTypeMapper.put(USER_DATA.getId(), USER_DATA);
    }

    public static MessageType from(byte id) {
        if (!idToTypeMapper.containsKey(id)) {
            return UNKNOWN;
        }

        return idToTypeMapper.get(id);
    }
}
