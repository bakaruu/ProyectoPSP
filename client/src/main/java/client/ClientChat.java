// src/main/java/client/ClientChat.java
package client;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * Lógica de chat: conecta al servidor, envía/recibe líneas de texto.
 * Ahora sólo trocea cuando realmente hay >1 trozo.
 */
public class ClientChat {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Tamaño máximo de cada trozo: 64 KB
    private static final int MAX_CHUNK = 64 * 1024;

    public void connect(LoginData data, Consumer<String> onReceive) throws IOException {
        socket = new Socket(data.getServerIp(), data.getServerPort());
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // enviamos nombre
        out.println(data.getUsername());

        // convertimos y enviamos AVATAR:
        Image img = data.getAvatar().getImage();
        BufferedImage bimg = new BufferedImage(img.getWidth(null), img.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bimg.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(bimg, "png", baos);
        String b64avatar = Base64.getEncoder().encodeToString(baos.toByteArray());
        out.println("AVATAR:" + data.getUsername() + ":" + b64avatar);

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

    /**
     * Envía un mensaje. Si es FILE:… y hay más de un trozo, lo trocea;
     * si solo hay un trozo, deja que llegue como FILE:… original.
     */
    public void sendMessage(String msg) {
        if (out == null) return;

        try {
            if (msg.startsWith("FILE:")) {
                // original: FILE:usuario:filename:base64data
                String[] parts = msg.split(":", 4);
                String user     = parts[1];
                String filename = parts[2];
                byte[] full     = Base64.getDecoder().decode(parts[3]);

                int totalChunks = (full.length + MAX_CHUNK - 1) / MAX_CHUNK;
                if (totalChunks <= 1) {
                    // un solo trozo: lo mando sin tocar
                    out.println(msg);
                    return;
                }

                // más de un trozo: lo partimos
                for (int i = 0; i < totalChunks; i++) {
                    int start = i * MAX_CHUNK;
                    int end   = Math.min(full.length, start + MAX_CHUNK);
                    byte[] chunk = Arrays.copyOfRange(full, start, end);
                    String b64chunk = Base64.getEncoder().encodeToString(chunk);
                    out.println(String.format(
                            "CHUNK:%s:%s:%d:%d:%s",
                            user, filename, i, totalChunks, b64chunk
                    ));
                }
            } else {
                // texto, avatar…
                out.println(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}
