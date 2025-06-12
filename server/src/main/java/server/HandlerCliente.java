// src/main/java/server/HandlerCliente.java
package server;

import java.io.*;
import java.net.Socket;

/**
 * Maneja cliente en servidor: texto, archivo y avatar en Base64.
 */
public class HandlerCliente implements Runnable {
    private Socket socket;
    private ChatServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String nombre;

    public HandlerCliente(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        try {
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            // 1. Nombre del usuario
            nombre = in.readLine();
            server.broadcast(nombre + " se ha unido al chat");

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("FILE:") || line.startsWith("AVATAR:")) {
                    // reenviar la línea cruda (archivo o avatar)
                    server.broadcast(line);
                } else {
                    // mensaje de texto normal: prefijar nick
                    server.broadcast(nombre + ": " + line);
                }
            }
        } catch (IOException e) {
            // ignore
        } finally {
            server.removeClient(this);
            server.broadcast(nombre + " ha abandonado el chat");
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /** Envía un mensaje a este cliente */
    public void send(String msg) {
        out.println(msg);
    }
}
