package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Ventana principal de chat: área de mensajes, cuadro de entrada y botón.
 */
public class ChatWindow extends JFrame {
    private JTextArea taChat   = new JTextArea();
    private JTextField tfInput = new JTextField(30);
    private JButton btnSend    = new JButton("Enviar");
    private ClientChat client  = new ClientChat();

    public ChatWindow(LoginData data) {
        super("Chat - " + data.getUsername());
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        taChat.setEditable(false);
        JScrollPane scroll = new JScrollPane(taChat);

        JPanel bottom = new JPanel();
        bottom.add(tfInput);
        bottom.add(btnSend);

        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(null);

        // Conectar
        try {
            client.connect(data, this::appendMessage);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error de conexión: " + ex.getMessage());
            System.exit(1);
        }

        // Evento botón y tecla Enter
        btnSend.addActionListener((ActionEvent e) -> sendText());
        tfInput.addActionListener((e) -> sendText());
    }

    private void sendText() {
        String text = tfInput.getText().trim();
        if (!text.isEmpty()) {
            client.sendMessage(text);
            tfInput.setText("");
        }
    }




    private void appendMessage(String msg) {
        SwingUtilities.invokeLater(() -> {
            taChat.append(msg + "\n");
            taChat.setCaretPosition(taChat.getDocument().getLength());
        });
    }
}
