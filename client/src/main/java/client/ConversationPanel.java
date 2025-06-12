// src/main/java/client/ConversationPanel.java
package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.Base64;

/**
 * Panel individual de conversación: texto, archivos e imágenes.
 * Ahora, si falla la conexión, pide al usuario verificar o corregir los datos
 * usando la caché o reabriendo el diálogo de login.
 */
public class ConversationPanel extends JPanel {
    private final ConversationLayout layout;
    private final ClientChat client = new ClientChat();
    /** Mensajes FILE: que hemos enviado y estamos esperando filtrar */
    private final Set<String> sentFiles = new HashSet<>();
    private LoginData data;
    private final Map<String, ImageIcon> avatars = new HashMap<>();

    public ConversationPanel(LoginData data) {
        super(new BorderLayout(5,5));
        this.data = data;
        layout = new ConversationLayout(data);
        add(layout, BorderLayout.CENTER);
        connect();
    }

    private void connect() {
        Consumer<String> onReceive = this::handleMessage;
        new Thread(() -> {
            try {
                client.connect(data, onReceive);
            } catch (IOException ex) {
                SwingUtilities.invokeLater(() -> {
                    int choice = JOptionPane.showOptionDialog(
                            this,
                            "No se pudo conectar a " + data.getServerIp() + ":" + data.getServerPort() +
                                    "\n¿Quieres verificar los datos de conexión?",
                            "Error de conexión",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.ERROR_MESSAGE,
                            null,
                            new String[]{"Verificar", "Cancelar"},
                            "Verificar"
                    );

                    if (choice == JOptionPane.YES_OPTION) {
                        List<String[]> cache = CacheManager.load();
                        String[] options = cache.stream()
                                .map(arr -> arr[0] + " @ " + arr[1] + ":" + arr[2])
                                .toArray(String[]::new);

                        String selection = null;
                        if (options.length > 0) {
                            selection = (String) JOptionPane.showInputDialog(
                                    this,
                                    "Selecciona una entrada guardada o presiona Cancelar para reingresar manualmente:",
                                    "Cache de conexiones",
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    options,
                                    options[0]
                            );
                        }

                        if (selection != null) {
                            String[] parts = selection.split(" @ ")[1].split(":");
                            data = new LoginData(
                                    data.getUsername(),
                                    data.getAvatar(),
                                    parts[0],
                                    Integer.parseInt(parts[1])
                            );
                            CacheManager.addAndSave(
                                    data.getUsername(),
                                    data.getServerIp(),
                                    data.getServerPort()
                            );
                            connect();
                        } else {
                            LoginData nuevo = LoginDialog.showDialog(
                                    (Frame) SwingUtilities.getWindowAncestor(this)
                            );
                            if (nuevo != null) {
                                data = nuevo;
                                CacheManager.addAndSave(
                                        data.getUsername(),
                                        data.getServerIp(),
                                        data.getServerPort()
                                );
                                connect();
                            }
                        }
                    }
                });
            }
        }, "Connector").start();
    }

    public void sendText(String text) {
        sendAvatar();
        client.sendMessage(text);
    }

    public void sendFile() {
        FileDialog fd = new FileDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Seleccionar archivo", FileDialog.LOAD
        );
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) return;
        try {
            sendAvatar();
            byte[] bytes = Files.readAllBytes(new File(dir, file).toPath());
            String b64 = Base64.getEncoder().encodeToString(bytes);
            String msg = String.format("FILE:%s:%s:%s",
                    data.getUsername(), file, b64);
            sentFiles.add(msg);
            client.sendMessage(msg);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Error leyendo archivo: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void sendAvatar() {
        try {
            Image img = data.getAvatar().getImage();
            BufferedImage bimg = new BufferedImage(
                    img.getWidth(null), img.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB
            );
            Graphics2D g = bimg.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bimg, "png", baos);
            String b64 = Base64.getEncoder().encodeToString(baos.toByteArray());
            client.sendMessage("AVATAR:" + data.getUsername() + ":" + b64);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                // ——— Filtrado e inserción de nuestros propios FILE: ———
                if (msg.startsWith("FILE:")) {
                    String[] parts = msg.split(":", 4);
                    if (parts.length != 4) return;
                    String sender    = parts[1];
                    String name      = parts[2];
                    byte[] dataBytes = Base64.getDecoder().decode(parts[3]);
                    boolean isOwn    = sender.equals(data.getUsername());
                    ImageIcon avatar = avatars.getOrDefault(sender, data.getAvatar());

                    // Insertar propio y descartar rebroadcast
                    if (isOwn && sentFiles.remove(msg)) {
                        BufferedImage img = null;
                        try {
                            img = ImageIO.read(new ByteArrayInputStream(dataBytes));
                        } catch (IOException ignored) {}
                        if (img != null) {
                            layout.insertImageMessage(sender, img, name, dataBytes, avatar, true);
                        } else {
                            layout.insertFileMessage(sender, name, dataBytes, avatar, true);
                        }
                        return;
                    }

                    // Insertar archivo recibido de otros
                    BufferedImage img = null;
                    try {
                        img = ImageIO.read(new ByteArrayInputStream(dataBytes));
                    } catch (IOException ignored) {}
                    if (img != null) {
                        layout.insertImageMessage(sender, img, name, dataBytes, avatar, false);
                    } else {
                        layout.insertFileMessage(sender, name, dataBytes, avatar, false);
                    }
                    return;
                }

                // ——— AVATAR: ———
                if (msg.startsWith("AVATAR:")) {
                    String[] p = msg.split(":", 3);
                    String user = p[1];
                    byte[] imgData = Base64.getDecoder().decode(p[2]);
                    try {
                        BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgData));
                        avatars.put(user, new ImageIcon(
                                img.getScaledInstance(40,40,Image.SCALE_SMOOTH)
                        ));
                    } catch (IOException ignored) {}
                }
                // ——— CHUNK: (se deja tu lógica de servidor) ———
                else if (msg.startsWith("CHUNK:")) {
                    // tu código no necesita cambio aquí
                }
                // ——— Texto y sistema ———
                else {
                    int idx = msg.indexOf(": ");
                    if (idx != -1) {
                        String sender = msg.substring(0, idx);
                        String body   = msg.substring(idx + 2);
                        boolean isOwn = sender.equals(data.getUsername());
                        ImageIcon avatar = avatars.getOrDefault(sender, data.getAvatar());
                        layout.insertMessage(sender, body, avatar, isOwn);
                    } else {
                        layout.insertSystemMessage(msg);
                    }
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveFile(String name, byte[] bytes) {
        FileDialog fd = new FileDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Guardar archivo", FileDialog.SAVE
        );
        fd.setFile(name);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            try (FileOutputStream fos = new FileOutputStream(new File(dir, file))) {
                fos.write(bytes);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        "Error guardando: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
