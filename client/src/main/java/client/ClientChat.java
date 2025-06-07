package client;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Lógica de chat: conecta al servidor, envía/recibe líneas de texto.
 */
public class ClientChat {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Abre la conexión y manda el nombre como primer mensaje.
     */
    public void connect(LoginData data, Consumer<String> onReceive) throws IOException {
        socket = new Socket(data.getServerIp(), data.getServerPort());
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // enviamos nombre
        out.println(data.getUsername());

        // hilo de lectura
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    onReceive.accept(line);
                }
            } catch (IOException ignored) {}
        }, "ClientReader").start();
    }

    /** Envía un mensaje de texto al servidor. */
    public void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    /** Cierra la conexión. */
    public void disconnect() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
