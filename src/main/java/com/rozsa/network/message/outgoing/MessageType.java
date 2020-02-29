package com.rozsa.network.message.outgoing;

import java.util.HashMap;

public enum MessageType {
    UNKNOWN(-1),
    CONNECTION_REQUEST(1),
    CONNECTION_RESPONSE(2),
    CONNECTION_DENIED(3),
    USER_DATA(4),
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
        idToTypeMapper.put(CONNECTION_REQUEST.getId(), CONNECTION_REQUEST);
        idToTypeMapper.put(CONNECTION_RESPONSE.getId(), CONNECTION_RESPONSE);
        idToTypeMapper.put(CONNECTION_DENIED.getId(), CONNECTION_DENIED);
        idToTypeMapper.put(USER_DATA.getId(), USER_DATA);
    }

    public static MessageType from(byte id) {
        if (!idToTypeMapper.containsKey(id)) {
            return UNKNOWN;
        }

        return idToTypeMapper.get(id);
    }
}
