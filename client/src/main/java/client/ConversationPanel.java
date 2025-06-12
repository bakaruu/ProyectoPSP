package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.Base64;

/**
 * Panel individual de conversacion: texto, archivos e imagenes.
 */
public class ConversationPanel extends JPanel {
    private final ConversationLayout layout;
    private final ClientChat client = new ClientChat();
    private final Set<String> sentFiles = new HashSet<>();
    private final LoginData data;
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
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        this, "Error de conexion: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE));
            }
        }, "Connector").start();
    }

    public void sendText(String text) {
        sendAvatar();
        client.sendMessage(text);
    }

    public void sendFile() {
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Seleccionar archivo", FileDialog.LOAD);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir == null || file == null) return;
        try {
            sendAvatar();
            byte[] bytes = Files.readAllBytes(new File(dir, file).toPath());
            String b64 = Base64.getEncoder().encodeToString(bytes);
            String msg = String.format("FILE:%s:%s:%s", data.getUsername(), file, b64);
            sentFiles.add(msg);
            client.sendMessage(msg);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error leyendo archivo: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendAvatar() {
        try {
            Image img = data.getAvatar().getImage();
            BufferedImage bimg = new BufferedImage(img.getWidth(null), img.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB);
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
                if (msg.startsWith("AVATAR:")) {
                    String[] p = msg.split(":", 3);
                    String user = p[1];
                    byte[] imgData = Base64.getDecoder().decode(p[2]);
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgData));
                    avatars.put(user, new ImageIcon(img.getScaledInstance(40,40,Image.SCALE_SMOOTH)));
                    return;
                }
                if (msg.startsWith("FILE:")) {
                    String[] parts = msg.split(":", 4);
                    if (parts.length != 4) return;
                    String sender = parts[1];
                    String name = parts[2];
                    byte[] dataBytes = Base64.getDecoder().decode(parts[3]);
                    boolean isOwn = sender.equals(data.getUsername());
                    ImageIcon avatar = avatars.getOrDefault(sender, data.getAvatar());

                    BufferedImage img = null;
                    try {
                        img = ImageIO.read(new ByteArrayInputStream(dataBytes));
                    } catch (IOException ignored) {}

                    if (img != null) {
                        layout.insertImageMessage(sender, img, name, dataBytes, avatar, isOwn);
                    } else {
                        layout.insertFileMessage(sender, name, dataBytes, avatar, isOwn);
                    }
                    return;
                }
                int idx = msg.indexOf(": ");
                if (idx != -1) {
                    String sender = msg.substring(0, idx);
                    String body = msg.substring(idx + 2);
                    boolean isOwn = sender.equals(data.getUsername());
                    ImageIcon avatar = avatars.getOrDefault(sender, data.getAvatar());
                    layout.insertMessage(sender, body, avatar, isOwn);
                } else {
                    layout.insertSystemMessage(msg);
                }
            } catch (BadLocationException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveFile(String name, byte[] bytes) {
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Guardar archivo", FileDialog.SAVE);
        fd.setFile(name);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            try (FileOutputStream fos = new FileOutputStream(new File(dir, file))) {
                fos.write(bytes);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error guardando: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}