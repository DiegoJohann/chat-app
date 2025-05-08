import command.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ConcurrentHashMap<String, ClientHandler> clientesOnline;
    private final Set<String> apelidos;
    private String apelido;
    private PrintWriter out;

    public ClientHandler(Socket socket, ConcurrentHashMap<String, ClientHandler> clientes, Set<String> apelidos) {
        this.socket = socket;
        this.clientesOnline = clientes;
        this.apelidos = apelidos;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            out = new PrintWriter(socket.getOutputStream(), true);
            String line;

            while ((line = in.readLine()) != null) {
                Command cmd = Command.fromMessage(line);
                if (cmd == null) continue;

                switch (cmd) {
                    case LOGIN:
                        handleLogin(line.substring(Command.LOGIN.getPrefix().length()));
                        break;
                    case MSG:
                        broadcast(Command.MSG.getPrefix() + apelido + ":" + line.substring(Command.MSG.getPrefix().length()));
                        break;
                    case PRIVADO:
                        String[] parts = line.split(":", 3);
                        envioPrivado(parts[1], Command.PRIVADO.getPrefix() + apelido + ":" + parts[2]);
                        break;
                    case SAIR:
                        return;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("Erro com cliente " + apelido);
        } finally {
            cleanup();
        }
    }

    private void handleLogin(String nick) {
        apelido = nick;
        if (clientesOnline.containsKey(apelido)) {
            out.println(Command.ERRO.getPrefix() + "nickname_em_uso");
        } else {
            apelidos.add(apelido);
            clientesOnline.put(apelido, this);
            out.println(Command.OK.getPrefix());
            enviaUsuariosPara(this);
            broadcast(Command.MSG.getPrefix() + "Servidor:" + apelido + " entrou no chat");
            atualizaUsuarios();
        }
    }

    private void broadcast(String msg) {
        clientesOnline.values().forEach(c -> c.out.println(msg));
    }

    private void envioPrivado(String dest, String msg) {
        ClientHandler client = clientesOnline.get(dest);
        if (client != null) {
            client.out.println(msg);
        } else {
            out.println(Command.MSG.getPrefix() + "Servidor:Usuário " + dest + " não encontrado.");
        }
    }

    private void atualizaUsuarios() {
        String userList = String.join(",", clientesOnline.keySet());
        clientesOnline.values().forEach(c -> c.out.println(Command.USUARIOS.getPrefix() + userList));
    }

    private void enviaUsuariosPara(ClientHandler destino) {
        String userList = String.join(",", apelidos);
        destino.out.println(Command.USUARIOS.getPrefix() + userList);
    }

    private void cleanup() {
        try { socket.close(); } catch (IOException ignored) {}
        if (apelido != null) {
            clientesOnline.remove(apelido);
            System.out.println("Cliente desconectado: " + apelido);
            broadcast(Command.MSG.getPrefix() + "Servidor:" + apelido + " saiu do chat");
            atualizaUsuarios();
        } else {
            System.out.println("Cliente desconectado antes de completar o login.");
        }
    }
}
