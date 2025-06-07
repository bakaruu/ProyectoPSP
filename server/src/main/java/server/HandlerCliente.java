package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Maneja la comunicación con un cliente conectado.
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
            System.err.println("Error al crear flujos: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // El primer mensaje es el nombre de usuario
            nombre = in.readLine();
            // Ahora broadcast solo recibe el String
            server.broadcast(nombre + " se ha unido al chat");

            String mensaje;
            while ((mensaje = in.readLine()) != null) {
                server.broadcast(nombre + ": " + mensaje);
            }
        } catch (IOException e) {
            System.err.println("Error en comunicación con cliente " + nombre + ": " + e.getMessage());
        } finally {
            desconectar();
        }
    }

    public void send(String message) {
        out.println(message);
    }

    private void desconectar() {
        server.removeClient(this);
        server.broadcast(nombre + " ha abandonado el chat");
        try {
            socket.close();
        } catch (IOException ignored) {}
    }
}
