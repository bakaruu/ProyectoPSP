// src/main/java/client/ChatWindow.java
package client;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
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
 * ChatWindow con soporte de texto e intercambio de archivos.
 * Previsualiza imagenes y muestra iconos para otros tipos.
 */
public class ChatWindow extends JFrame {
    private final Set<String> sentFiles = new HashSet<>();
    private final JTextPane tpChat = new JTextPane();
    private final JTextField tfInput = new JTextField();
    private final JButton btnSend = new JButton("Enviar");
    private final JButton btnFile = new JButton("Enviar archivo");
    private final ClientChat client = new ClientChat();

    public ChatWindow(LoginData data) {
        super("Chat - " + data.getUsername());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5,5));
        ((JComponent)getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10,10,10,10)
        );

        tpChat.setEditable(false);
        JScrollPane scroll = new JScrollPane(tpChat);
        add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(5,5));
        bottom.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
        bottom.add(btnFile, BorderLayout.WEST);
        tfInput.setColumns(30);
        bottom.add(tfInput, BorderLayout.CENTER);
        bottom.add(btnSend, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);

        pack();
        setSize(600, 400);
        setLocationRelativeTo(null);

        Consumer<String> onReceive = this::handleMessage;
        new Thread(() -> {
            try {
                client.connect(data, onReceive);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        this,
                        "Error de conexion: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                ));
            }
        }, "Connector").start();

        btnSend.addActionListener(e -> sendText());
        tfInput.addActionListener(e -> sendText());
        btnFile.addActionListener(e -> sendFile());
    }

    private void sendText() {
        String text = tfInput.getText().trim();
        if (!text.isEmpty()) {
            client.sendMessage(text);
            tfInput.setText("");
        }
    }

    private void sendFile() {
        FileDialog fd = new FileDialog(this, "Seleccionar archivo", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> true);
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
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void handleMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            try {
                StyledDocument doc = tpChat.getStyledDocument();
                if (msg.startsWith("FILE:")) {
                    boolean isLocal = sentFiles.remove(msg);
                    String[] parts = msg.split(":", 3);
                    if (parts.length != 3) return;
                    String name = parts[1];
                    byte[] data = Base64.getDecoder().decode(parts[2]);

                    // Salto de linea antes de insertar archivo
                    doc.insertString(doc.getLength(), "\n", null);
                    tpChat.setCaretPosition(doc.getLength());

                    // Intentar cargar como imagen
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                    if (img != null) {
                        // Previsualizar imagen
                        int max = 100;
                        int w = img.getWidth(), h = img.getHeight();
                        double ratio = Math.min((double)max / w, (double)max / h);
                        int nw = (int)(w * ratio), nh = (int)(h * ratio);
                        ImageIcon icon = new ImageIcon(
                                img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH)
                        );
                        JLabel imgLabel = new JLabel(icon);
                        imgLabel.setCursor(
                                Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        );
                        imgLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                            @Override
                            public void mouseClicked(java.awt.event.MouseEvent e) {
                                JFrame frame = new JFrame(name);
                                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                JLabel full = new JLabel(new ImageIcon(img));
                                JScrollPane pane = new JScrollPane(full);
                                frame.add(pane, BorderLayout.CENTER);
                                Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
                                int margin = 50;
                                frame.setSize(
                                        screen.width - margin,
                                        screen.height - margin
                                );
                                frame.setLocationRelativeTo(null);
                                frame.setVisible(true);
                            }
                        });
                        tpChat.insertComponent(imgLabel);
                        doc.insertString(doc.getLength(), "\n", null);
                        if (!isLocal) {
                            JButton btn = new JButton("Guardar");
                            btn.addActionListener(e -> saveImage(name, data));
                            tpChat.insertComponent(btn);
                            doc.insertString(doc.getLength(), "\n", null);
                        }
                    } else {
                        // No es imagen: mostrar icono generico y nombre
                        Icon fileIcon = UIManager.getIcon("FileView.fileIcon");
                        JLabel fileLabel = new JLabel(name, fileIcon, JLabel.LEFT);
                        tpChat.insertComponent(fileLabel);
                        // salto de linea tras etiqueta
                        doc.insertString(doc.getLength(), "\n", null);
                        if (!isLocal) {
                            JButton btn = new JButton("Guardar");
                            btn.addActionListener(e -> saveImage(name, data));
                            tpChat.insertComponent(btn);
                            doc.insertString(doc.getLength(), "\n", null);
                        }
                    }
                } else {
                    // Texto normal
                    doc.insertString(doc.getLength(), msg + "\n", null);
                    tpChat.setCaretPosition(doc.getLength());
                }
            } catch (BadLocationException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void saveImage(String name, byte[] data) {
        FileDialog fd = new FileDialog(this, "Guardar archivo", FileDialog.SAVE);
        fd.setFile(name);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            File outFile = new File(dir, file);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                fos.write(data);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error guardando: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
