// src/main/java/client/ConversationPanel.java
package client;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import javax.imageio.ImageIO;

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

    // Invocado desde ChatWindow cuando pulsas Enviar en esta pestaña
    public void sendText(String text) {
        // 1) Manda el avatar
        try {
            // Convierte tu ImageIcon a BufferedImage
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
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // 2) Manda el texto
        client.sendMessage(text);
    }


    public void sendFile() {
        FileDialog fd = new FileDialog(
                (Frame)SwingUtilities.getWindowAncestor(this), "Seleccionar archivo", FileDialog.LOAD);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            File f = new File(dir, file);
            try {
                byte[] bytes = Files.readAllBytes(f.toPath());
                String b64 = Base64.getEncoder().encodeToString(bytes);
                String msg = "FILE:" + f.getName() + ":" + b64;
                sentFiles.add(msg);
                client.sendMessage(msg);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error leyendo archivo: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = layout.getDocument();

                // 1) AVATAR:usuario:BASE64 → guardamos y salimos
                if (msg.startsWith("AVATAR:")) {
                    String[] p = msg.split(":", 3);
                    String user = p[1];
                    byte[] imgData = Base64.getDecoder().decode(p[2]);
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgData));
                    ImageIcon icon = new ImageIcon(
                            img.getScaledInstance(40, 40, Image.SCALE_SMOOTH)
                    );
                    avatars.put(user, icon);
                    return;
                }

                // 2) FILE:… → manejo de archivos/img locales y remotos
                if (msg.startsWith("FILE:")) {
                    boolean isLocal = sentFiles.remove(msg);
                    String[] parts = msg.split(":", 3);
                    if (parts.length != 3) return;
                    String name = parts[1];
                    byte[] data = Base64.getDecoder().decode(parts[2]);

                    doc.insertString(doc.getLength(), "\n", null);
                    layout.insertCaret();
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                    if (img != null) {
                        layout.insertImage(img, name, data, isLocal);
                    } else {
                        layout.insertFileIcon(name, data, isLocal);
                    }
                    return;
                }

                // 3) Texto normal o mensaje de sistema
                int idx = msg.indexOf(": ");
                if (idx != -1) {
                    // Mensaje de usuario: "usuario: texto…"
                    String sender = msg.substring(0, idx);
                    String body   = msg.substring(idx + 2);
                    ImageIcon icon = avatars.getOrDefault(sender, data.getAvatar());
                    layout.insertMessage(sender, body, icon);
                } else {
                    // Mensaje de sistema: "X se ha unido…" o "X ha abandonado…"
                    layout.insertSystemMessage(msg);
                }

            } catch (BadLocationException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveFile(String name, byte[] data) {
        FileDialog fd = new FileDialog((Frame)SwingUtilities.getWindowAncestor(this), "Guardar archivo", FileDialog.SAVE);
        fd.setFile(name);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            try (FileOutputStream fos = new FileOutputStream(new File(dir, file))) {
                fos.write(data);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "Error guardando: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

