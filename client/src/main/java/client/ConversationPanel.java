// src/main/java/client/ConversationPanel.java
package client;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
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
                if (msg.startsWith("FILE:")) {
                    boolean isLocal = sentFiles.remove(msg);
                    String[] parts = msg.split(":",3);
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
                } else {
                    doc.insertString(doc.getLength(), msg + "\n", null);
                    layout.insertCaret();
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

