package server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.Base64;

/**
 * Maneja cliente en servidor: texto, archivo y avatar.
 * Ahora notifica cada trozo a emisor y receptores.
 */
public class HandlerCliente implements Runnable {
    private Socket socket;
    private ChatServer server;
    private BufferedReader in;
    private PrintWriter out;
    private String nombre;

    // Acumulador de trozos: clave "usuario:filename"
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
                // ——— Procesamos chunks ► envío trozo a trozo ———
                if (line.startsWith("CHUNK:")) {
                    // Parseo de los campos
                    String[] p = line.split(":", 6);
                    String user     = p[1];
                    String filename = p[2];
                    int idx         = Integer.parseInt(p[3]);
                    int total       = Integer.parseInt(p[4]);
                    byte[] data     = Base64.getDecoder().decode(p[5]);

                    String key = user + ":" + filename;
                    // guardamos el trozo
                    incomingChunks
                            .computeIfAbsent(key, k ->
                                    new ArrayList<>(Collections.nCopies(total, null)))
                            .set(idx, data);

                    List<byte[]> parts = incomingChunks.get(key);

                    if (total > 1) {
                        // 1) Al emisor: notificamos cada trozo enviado
                        this.send(String.format(
                                "[Sistema] Trozo %d/%d enviado (%d bytes)",
                                idx+1, total, data.length));

                        // 2) Al resto: notificamos cada trozo recibido
                        server.broadcastExcept(
                                String.format("[Sistema] Trozo %d/%d recibido", idx+1, total),
                                this
                        );
                    }

                    // Si ya recibimos todos, reensamblamos
                    if (!parts.contains(null)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        for (byte[] part : parts) baos.write(part);
                        byte[] full = baos.toByteArray();
                        incomingChunks.remove(key);

                        // Avisos de reconstrucción
                        if (total > 1) {
                            this.send("[Sistema] Hecho");
                            server.broadcastExcept(
                                    "[Sistema] Reconstruyendo y mostrando archivo...",
                                    this
                            );
                        }

                        // Envío final del FILE completo
                        String b64full = Base64.getEncoder().encodeToString(full);
                        server.broadcast("FILE:" + user + ":" + filename + ":" + b64full);

                        // Mensajes de cierre
                        if (total > 1) {
                            this.send("[Sistema] Envio de \"" + filename + "\" completado");
                            server.broadcastExcept(
                                    "[Sistema] Archivo \"" + filename + "\" recibido correctamente",
                                    this
                            );
                        }
                    }

                    continue;  // saltamos las demás ramas
                }

                // ——— Resto de casos ———
                if (line.startsWith("FILE:") || line.startsWith("AVATAR:")) {
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

    /** Envía un mensaje a este cliente. */
    public void send(String msg) {
        out.println(msg);
    }
}
