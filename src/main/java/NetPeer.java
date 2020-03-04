import com.rozsa.network.message.incoming.ConnectedMessage;
import com.rozsa.network.message.incoming.DisconnectedMessage;
import com.rozsa.network.message.incoming.IncomingMessage;
import com.rozsa.network.Peer;
import com.rozsa.network.PeerConfig;

import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class NetPeer {
    private final Peer peer;
    private final Set<Consumer<ConnectedMessage>> onConnectedEventListeners;
    private final Set<Consumer<DisconnectedMessage>> onDisconnectedEventListeners;

    public NetPeer(PeerConfig config) throws SocketException {
        peer = new Peer(config);
        onConnectedEventListeners = new HashSet<>();
        onDisconnectedEventListeners = new HashSet<>();
    }

    public void initialize() {
        peer.initialize();
    }

    public void addOnConnectedEventListener(Consumer<ConnectedMessage> listener) {
        onConnectedEventListeners.add(listener);
    }

    public void removeOnConnectedEventListener(Consumer<ConnectedMessage> listener) {
        onConnectedEventListeners.remove(listener);
    }

    public void addOnDisconnectedEventListener(Consumer<DisconnectedMessage> listener) {
        onDisconnectedEventListeners.add(listener);
    }

    public void removeOnDisconnectedEventListener(Consumer<DisconnectedMessage> listener) {
        onDisconnectedEventListeners.remove(listener);
    }

    public void connect(String ip, int port) throws NotActiveException, UnknownHostException {
        peer.connect(ip, port);
    }

    public IncomingMessage receive() {
        IncomingMessage msg = peer.read();
        if (msg == null) {
            return null;
        }

        switch (msg.getType()) {
            // TODO: create a connection state changed
            case CONNECTED:
                handleConnectedMessage(msg);
                break;
            case DISCONNECTED:
                handleDisconnectedMessage(msg);
                break;
            case USER_DATA:
                return msg;
            default:
                break;
        }

        return null;
    }

    private void handleConnectedMessage(IncomingMessage msg) {
        onConnectedEventListeners.forEach(l -> l.accept((ConnectedMessage)msg));
    }

    private void handleDisconnectedMessage(IncomingMessage msg) {
        onDisconnectedEventListeners.forEach(l -> l.accept((DisconnectedMessage)msg));
    }
}
