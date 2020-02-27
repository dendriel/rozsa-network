import com.rozsa.network.IncomingMessage;
import com.rozsa.network.Peer;
import com.rozsa.network.PeerConfig;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;

// Testing class.
class NetworkPeer {
    private int port;
    private Peer peer;

    NetworkPeer(int port) throws SocketException {
        this.port = port;
        peer = new Peer(new PeerConfig(port));
        peer.initialize();
    }

    public void start() {
        new Thread(this::loop).start();
        System.out.println("Peer started at port " + port);
    }

    public void connect(int targetPort) throws NotActiveException, UnknownHostException {
        peer.connect("localhost", targetPort);
    }

    private void loop() {
        while(true) {
            try {
                recvIncomingMessages();
            } catch (InterruptedException e) {
                e.printStackTrace();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void recvIncomingMessages() throws InterruptedException {
        IncomingMessage msg = peer.read();
        if (msg == null) {
            Thread.sleep(1);
            return;
        }

        System.out.println("Received message " + msg);
    }
}
