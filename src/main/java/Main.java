import java.io.NotActiveException;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Main {
    public static void main(String[] args) throws SocketException, NotActiveException, UnknownHostException {
        int serverPort = 9090;
        NetworkPeer server = new NetworkPeer(serverPort);
        server.start();

        int clientPort = 8989;
        NetworkPeer client = new NetworkPeer(clientPort);
        client.start();
        client.connect(serverPort);
    }
}
