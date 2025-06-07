package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ChatServer: acepta conexiones de clientes y distribuye mensajes.
 */
public class ChatServer {
    private int port;
    private Set<HandlerCliente> clients = Collections.synchronizedSet(new HashSet<>());

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Servidor iniciado en puerto " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                HandlerCliente handler = new HandlerCliente(socket, this);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.err.println("Error en el servidor: " + e.getMessage());
        }
    }

    /** Envía un mensaje a todos (excepto el remitente). */
    public void broadcast(String message) {
        synchronized (clients) {
            for (HandlerCliente client : clients) {
                client.send(message);
            }
        }
    }

    public void removeClient(HandlerCliente handler) {
        clients.remove(handler);
    }

    public static void main(String[] args) {
        int puerto = 12345;  // Puedes cambiarlo o pasarlo como argumento
        new ChatServer(puerto).start();
    }
}
