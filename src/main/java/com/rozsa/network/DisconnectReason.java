package com.rozsa.network;

import java.util.HashMap;
import java.util.Map;

/**
 * Reasons for a disconnection.
 */
public enum DisconnectReason {
    /**
     * Open.
     */
    NONE(0),
    /**
     * Closed by local peer.
     */
    LOCAL_CLOSE(1),
    /**
     * Closed by remote peer.
     */
    REMOTE_CLOSE(2),
    /**
     * Didn't receive ping inside expected time interval.
     */
    TIMEOUT(3),
    /**
     * Denied by remote peer.
     */
    DENIED(4),
    /**
     * Remote peer didn't responded to connection request.
     */
    NO_RESPONSE(5),

    /**
     * Custom user disconnection reasons.
     */
    CUSTOM_USER_01(6),
    CUSTOM_USER_02(7),
    CUSTOM_USER_03(8),
    CUSTOM_USER_04(9),
    CUSTOM_USER_05(10),
    CUSTOM_USER_06(11),
    CUSTOM_USER_07(12),
    CUSTOM_USER_08(13),
    ;

    private byte id;
    DisconnectReason(int id) {
        this.id = (byte)id;
    }

    public byte getId() {
        return id;
    }

    private static Map<Byte, DisconnectReason> idToReasonMapper;

    static {
        idToReasonMapper = new HashMap<>();
        idToReasonMapper.put(NONE.getId(), NONE);
        idToReasonMapper.put(LOCAL_CLOSE.getId(), LOCAL_CLOSE);
        idToReasonMapper.put(REMOTE_CLOSE.getId(), REMOTE_CLOSE);
        idToReasonMapper.put(TIMEOUT.getId(), TIMEOUT);
        idToReasonMapper.put(DENIED.getId(), DENIED);
        idToReasonMapper.put(NO_RESPONSE.getId(), NO_RESPONSE);
        idToReasonMapper.put(CUSTOM_USER_01.getId(), CUSTOM_USER_01);
        idToReasonMapper.put(CUSTOM_USER_02.getId(), CUSTOM_USER_02);
        idToReasonMapper.put(CUSTOM_USER_03.getId(), CUSTOM_USER_03);
        idToReasonMapper.put(CUSTOM_USER_04.getId(), CUSTOM_USER_04);
        idToReasonMapper.put(CUSTOM_USER_05.getId(), CUSTOM_USER_05);
        idToReasonMapper.put(CUSTOM_USER_06.getId(), CUSTOM_USER_06);
        idToReasonMapper.put(CUSTOM_USER_07.getId(), CUSTOM_USER_07);
        idToReasonMapper.put(CUSTOM_USER_08.getId(), CUSTOM_USER_08);
    }

    public static DisconnectReason from(Byte id) {
        return idToReasonMapper.getOrDefault(id, NONE);
    }
}
