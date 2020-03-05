package com.rozsa.network.channel;

import com.rozsa.network.message.outgoing.MessageType;

import java.util.HashMap;

public enum DeliveryMethod {
    UNRELIABLE(0),
    UNRELIABLE_SEQUENCED(1),
    RELIABLE(2),
    RELIABLE_SEQUENCE(3),
    RELIABLE_ORDERED(4),
    ;


    private final byte id;

    DeliveryMethod(int id) {
        this.id = (byte)id;
    }

    public byte getId() {
        return id;
    }

    private static HashMap<Byte, DeliveryMethod> idToTypeMapper;

    static {
        idToTypeMapper = new HashMap<>();
        idToTypeMapper.put(UNRELIABLE.getId(), UNRELIABLE);
        idToTypeMapper.put(UNRELIABLE_SEQUENCED.getId(), UNRELIABLE_SEQUENCED);
        idToTypeMapper.put(RELIABLE.getId(), RELIABLE);
        idToTypeMapper.put(RELIABLE_SEQUENCE.getId(), RELIABLE_SEQUENCE);
        idToTypeMapper.put(RELIABLE_ORDERED.getId(), RELIABLE_ORDERED);
    }

    public static DeliveryMethod from(byte id) {
        if (!idToTypeMapper.containsKey(id)) {
            return UNRELIABLE;
        }

        return idToTypeMapper.get(id);
    }
}
