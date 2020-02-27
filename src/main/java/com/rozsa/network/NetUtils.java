package com.rozsa.network;

import sun.net.util.IPAddressUtil;

public class NetUtils {
    public static final String LOOPBACK = "0.0.0.0";
    public static final String LOCALHOST_STR = "localhost";
    public static final String LOCALHOST = "127.0.0.1";

    public static long encodeAddress(String ip, int port) {
        long encoded = encodeIp(ip);
        encoded = encoded << 16 | (port & 0xFFFF);

        return encoded;
    }

    public static long encodeIp(String ip) {
        long encoded = 0;
        byte[] bytes = IPAddressUtil.textToNumericFormatV4(ip);
        if (bytes == null) {
            return encoded;
        }

        for (byte b : bytes) {
            encoded = encoded << 8 | (b & 0xFF);
        }

        return encoded;
    }
}
