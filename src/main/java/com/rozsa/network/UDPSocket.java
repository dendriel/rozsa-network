package com.rozsa.network;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

class UDPSocket {
    private DatagramSocket socket;

    private byte[] buf;

    UDPSocket(int port, int pollTimeInMillis, int recvBufferSize) throws SocketException {
        socket = new DatagramSocket(port);
        socket.setSoTimeout(pollTimeInMillis);
        buf = new byte[recvBufferSize];

        Logger.info("Socket bound at %s:%d", socket.getLocalAddress().getHostAddress(), socket.getLocalPort());
    }

    void send(InetAddress netAddr, int port, byte[] data, int dataLen) {
        DatagramPacket packet = new DatagramPacket(data, dataLen, netAddr, port);

        try {
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Receives a datagram packet. The data buffer should be copy, if necessary, before calling this method again.
     * @return The received datagram packet, if available; null otherwise.
     */
    DatagramPacket receive() {
        Arrays.fill(buf, (byte)0);
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            socket.receive(packet);
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return packet;
    }
}
