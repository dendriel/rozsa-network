import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws SocketException, NotActiveException, UnknownHostException {
        int serverPort = 9090;
        int clientPort = 8989;
        boolean isServer = false;

        isServer = true;

        if (isServer) {
            NetworkPeer server = new NetworkPeer(serverPort);
            server.start();
        }
        else {
            NetworkPeer client = new NetworkPeer(clientPort);
            client.start();
            client.connect(serverPort);
        }
    }
}
