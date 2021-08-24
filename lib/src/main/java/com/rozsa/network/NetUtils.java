package com.rozsa.network;

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
        byte[] bytes = textToNumericFormatV4(ip);
        if (bytes == null) {
            return encoded;
        }

        for (byte b : bytes) {
            encoded = encoded << 8 | (b & 0xFF);
        }

        return encoded;
    }

    // extracted from sun.net.util.IPAddressUtil
    private static byte[] textToNumericFormatV4(String var0) {
        byte[] var1 = new byte[4];
        long var2 = 0L;
        int var4 = 0;
        boolean var5 = true;
        int var6 = var0.length();
        if (var6 != 0 && var6 <= 15) {
            for(int var7 = 0; var7 < var6; ++var7) {
                char var8 = var0.charAt(var7);
                if (var8 == '.') {
                    if (var5 || var2 < 0L || var2 > 255L || var4 == 3) {
                        return null;
                    }

                    var1[var4++] = (byte)((int)(var2 & 255L));
                    var2 = 0L;
                    var5 = true;
                } else {
                    int var9 = Character.digit(var8, 10);
                    if (var9 < 0) {
                        return null;
                    }

                    var2 *= 10L;
                    var2 += (long)var9;
                    var5 = false;
                }
            }

            if (!var5 && var2 >= 0L && var2 < 1L << (4 - var4) * 8) {
                switch(var4) {
                    case 0:
                        var1[0] = (byte)((int)(var2 >> 24 & 255L));
                    case 1:
                        var1[1] = (byte)((int)(var2 >> 16 & 255L));
                    case 2:
                        var1[2] = (byte)((int)(var2 >> 8 & 255L));
                    case 3:
                        var1[3] = (byte)((int)(var2 >> 0 & 255L));
                    default:
                        return var1;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
