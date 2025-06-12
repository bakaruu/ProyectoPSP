// src/main/java/server/HandlerCliente.java
package server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.Base64;

/**
 * Maneja cliente en servidor: texto, archivo y avatar.
 * Ahora sólo emite mensajes de sistema cuando hay >1 trozo.
 */
public class HandlerCliente implements Runnable {
    private Socket socket;
    private ChatServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String nombre;

    // Acumulador de trozos entrantes: clave "usuario:filename"
    private final Map<String, List<byte[]>> incomingChunks = new HashMap<>();

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
            nombre = in.readLine();
            server.broadcast(nombre + " se ha unido al chat");

            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("CHUNK:")) {
                    String[] p = line.split(":", 6);
                    String user     = p[1];
                    String filename = p[2];
                    int idx         = Integer.parseInt(p[3]);
                    int total       = Integer.parseInt(p[4]);
                    byte[] data     = Base64.getDecoder().decode(p[5]);

                    String key = user + ":" + filename;
                    // guardamos el chunk
                    incomingChunks
                            .computeIfAbsent(key, k ->
                                    new ArrayList<>(Collections.nCopies(total, null)))
                            .set(idx, data);

                    List<byte[]> parts = incomingChunks.get(key);
                    // sólo si total>1 mostramos mensajes
                    if (total > 1) {
                        if (idx == 0) {
                            server.broadcast("[Sistema] Iniciando troceado de \""
                                    + filename + "\" en " + total + " trozos");
                        }
                        if (!parts.contains(null)) {
                            server.broadcast("[Sistema] Completados los "
                                    + total + " trozos de \"" + filename + "\""
                                    + "\nReconstruyendo y enviando archivo...");
                        }
                    }

                    // cuando ya tenemos todo, reensamblamos
                    if (!parts.contains(null)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        for (byte[] part : parts) baos.write(part);
                        byte[] full = baos.toByteArray();
                        incomingChunks.remove(key);

                        // enviamos FILE: igual que antes
                        String b64full = Base64.getEncoder().encodeToString(full);
                        server.broadcast("FILE:" + user + ":" + filename + ":" + b64full);

                        if (total > 1) {
                            server.broadcast("[Sistema] Envio de \"" + filename + "\" completado");
                        }
                    }

                } else if (line.startsWith("FILE:") || line.startsWith("AVATAR:")) {
                    // reenvío directo (archivos pequeños/avatares)
                    server.broadcast(line);

                } else {
                    server.broadcast(nombre + ": " + line);
                }
            }
        } catch (IOException ignored) {
        } finally {
            server.removeClient(this);
            server.broadcast(nombre + " ha abandonado el chat");
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void send(String msg) {
        out.println(msg);
    }
}
