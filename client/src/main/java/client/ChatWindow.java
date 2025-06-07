// src/main/java/client/ChatWindow.java
package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Ventana principal de chat: area de mensajes, cuadro de entrada,
 * boton de envio de texto y boton de envio de archivos.
 */
public class ChatWindow extends JFrame {
    private final JTextArea taChat   = new JTextArea();
    private final JTextField tfInput = new JTextField();
    private final JButton btnSend    = new JButton("Enviar");
    private final JButton btnFile    = new JButton("Enviar archivo");
    private final ClientChat client  = new ClientChat();

    public ChatWindow(LoginData data) {
        super("Chat - " + data.getUsername());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        // Layout principal con espacios entre componentes
        setLayout(new BorderLayout(5, 5));
        // Margen alrededor de todo el contenido
        ((JComponent) getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        );

        // Area de chat central
        taChat.setEditable(false);
        taChat.setFont(taChat.getFont().deriveFont(14f));
        JScrollPane scroll = new JScrollPane(taChat);
        add(scroll, BorderLayout.CENTER);

        // Panel inferior con boton archivo a la izquierda,
        // campo de texto expansible y boton Enviar a la derecha
        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        // Margen interno del panel inferior
        bottom.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Boton "Enviar archivo" al oeste
        bottom.add(btnFile, BorderLayout.WEST);

        // Campo de texto en el centro
        tfInput.setColumns(30);
        bottom.add(tfInput, BorderLayout.CENTER);

        // Boton "Enviar" al este
        bottom.add(btnSend, BorderLayout.EAST);

        add(bottom, BorderLayout.SOUTH);

        // Tamano inicial
        pack();
        setSize(600, 400);
        setMinimumSize(new Dimension(400, 300));
        setLocationRelativeTo(null);

        // Conectar al servidor en segundo plano
        new Thread(() -> {
            try {
                client.connect(data, this::appendMessage);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        this,
                        "Error de conexion: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                ));
            }
        }, "Connector").start();

        // Eventos
        btnSend.addActionListener((ActionEvent e) -> sendText());
        tfInput.addActionListener((e) -> sendText());
        btnFile.addActionListener((ActionEvent e) -> sendFile());
    }

    private void sendText() {
        String text = tfInput.getText().trim();
        if (!text.isEmpty()) {
            client.sendMessage(text);
            tfInput.setText("");
        }
    }

    /**
     * Placeholder para futura logica de envio de archivos.
     */
    private void sendFile() {
        JOptionPane.showMessageDialog(
                this,
                "Funcionalidad de envio de archivos aun no implementada.",
                "Info",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            taChat.append(msg + "\n");
            taChat.setCaretPosition(taChat.getDocument().getLength());
        });
    }
}
