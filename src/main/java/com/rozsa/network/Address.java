package com.rozsa.network;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static com.rozsa.network.NetUtils.*;

public class Address {
    private final InetAddress netAddress;

    private final int port;

    private final long id;

    public static Address from(String ip, int port) throws UnknownHostException {
        ip = ip.equals(LOCALHOST_STR) || ip.equals(LOOPBACK) ? LOCALHOST : ip;
        InetAddress netAddress = InetAddress.getByName(ip);
        long id = NetUtils.encodeAddress(ip, port);

        return new Address(id, netAddress, port);
    }

    public static Address from(InetAddress netAddress, int port) {
        long id = NetUtils.encodeAddress(netAddress.getHostAddress(), port);
        return new Address(id, netAddress, port);
    }

    private Address(long id, InetAddress netAddress, int port) {
        this.id = id;
        this.netAddress = netAddress;
        this.port = port;
    }

    public InetAddress getNetAddress() {
        return netAddress;
    }

    public String getIp() {
        return netAddress.getHostAddress();
    }

    public int getPort() {
        return port;
    }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Address{" +
                "netAddress=" + netAddress +
                ", port=" + port +
                ", id=" + id +
                '}';
    }
}
