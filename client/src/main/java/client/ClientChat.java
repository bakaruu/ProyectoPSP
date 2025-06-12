package client;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.Base64;
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

        // Sustituye EL CAST que daba error por este bloque:
        // -------------------------------------------------
        // Convierte el ImageIcon en BufferedImage
        Image img = data.getAvatar().getImage();
        int w = img.getWidth(null), h = img.getHeight(null);
        BufferedImage bimg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bimg.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();

        // Codifica a Base64 y envía AVATAR:username:...
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bimg, "png", baos);
        String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
        out.println("AVATAR:" + data.getUsername() + ":" + b64);
        // -------------------------------------------------
        // ────────────────────────────────────────────

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
