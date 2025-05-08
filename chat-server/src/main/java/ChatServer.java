import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final Set<String> apelidos = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, ClientHandler> clientesOnline = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8089);
        System.out.println("Servidor iniciado na porta 8089...");

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(new ClientHandler(clientSocket, clientesOnline, apelidos)).start();
        }
    }
}
