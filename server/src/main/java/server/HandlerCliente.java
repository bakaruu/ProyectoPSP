package server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.Base64;

/**
 * Maneja cliente en servidor: texto, archivo y avatar.
 * Ahora emite enviando… solo al emisor y recibiendo… al resto.
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
                    // Formato: CHUNK:usuario:filename:idx:total:b64chunk
                    String[] p = line.split(":", 6);
                    String user     = p[1];
                    String filename = p[2];
                    int idx         = Integer.parseInt(p[3]);
                    int total       = Integer.parseInt(p[4]);
                    byte[] data     = Base64.getDecoder().decode(p[5]);

                    String key = user + ":" + filename;
                    // guardamos el chunk en su posición
                    incomingChunks
                            .computeIfAbsent(key, k ->
                                    new ArrayList<>(Collections.nCopies(total, null)))
                            .set(idx, data);

                    List<byte[]> parts = incomingChunks.get(key);

                    if (total > 1) {
                        // --- Mensajes de progreso al propio emisor ---
                        if (idx == 0) {
                            this.send("[Sistema] Iniciando troceado de \"" +
                                    filename + "\" en " + total + " trozos");
                        }
                        if (!parts.contains(null)) {
                            this.send("[Sistema] Completados los " + total +
                                    " trozos de \"" + filename + "\"" +
                                    "\nReconstruyendo y enviando archivo...");
                        }

                        // --- Mensajes de progreso al resto de clientes ---
                        if (idx == 0) {
                            server.broadcastExcept(
                                    "[Sistema] Preparando recepción de \"" +
                                            filename + "\" en " + total + " trozos",
                                    this
                            );
                        }
                        if (!parts.contains(null)) {
                            server.broadcastExcept(
                                    "[Sistema] Recibidos los " + total +
                                            " trozos de \"" + filename + "\"" +
                                            "\nReconstruyendo y mostrando archivo...",
                                    this
                            );
                        }
                    }

                    // Cuando ya tenemos todos los trozos:
                    if (!parts.contains(null)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        for (byte[] part : parts) baos.write(part);
                        byte[] full = baos.toByteArray();
                        incomingChunks.remove(key);

                        String b64full = Base64.getEncoder().encodeToString(full);
                        // Enviamos el archivo completo a todos
                        server.broadcast("FILE:" + user + ":" + filename + ":" + b64full);

                        if (total > 1) {
                            // Aviso de finalización al emisor
                            this.send("[Sistema] Envío de \"" + filename + "\" completado");
                            // Aviso de recepción al resto
                            server.broadcastExcept(
                                    "[Sistema] Archivo \"" + filename + "\" recibido correctamente",
                                    this
                            );
                        }
                    }

                } else if (line.startsWith("FILE:") || line.startsWith("AVATAR:")) {
                    // reenvío directo (archivos pequeños y avatares)
                    server.broadcast(line);

                } else {
                    // texto normal
                    server.broadcast(nombre + ": " + line);
                }
            }
        } catch (IOException ignored) {
            // cliente desconectado
        } finally {
            server.removeClient(this);
            server.broadcast(nombre + " ha abandonado el chat");
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /** Envía un mensaje a este cliente. */
    public void send(String msg) {
        out.println(msg);
    }
}
