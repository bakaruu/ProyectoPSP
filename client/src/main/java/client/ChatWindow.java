package client;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.io.ByteArrayInputStream;

/**
 * ChatWindow que gestiona múltiples conversaciones en pestañas.
 */
public class ChatWindow extends JFrame {
    private final JTabbedPane tabs = new JTabbedPane();
    private final JButton btnNew   = new JButton("Nuevo chat");
    private final JButton btnFile  = new JButton("Enviar archivo");
    private final JTextField tfInput = new JTextField(30);
    private final JButton btnSend  = new JButton("Enviar");

    public ChatWindow() {
        super("Chat Multisesión");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(5,5));
        ((JComponent)getContentPane()).setBorder(
                BorderFactory.createEmptyBorder(10,10,10,10)
        );

        // NORTH: Nuevo Chat
        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(btnNew);
        add(north, BorderLayout.NORTH);

        // CENTER: pestañas de conversaciones
        add(tabs, BorderLayout.CENTER);

        // SOUTH: panel de envío
        JPanel south = new JPanel(new BorderLayout(5,5));
        JPanel west = new JPanel(new FlowLayout(FlowLayout.LEFT,5,0));
        west.add(btnFile);
        south.add(west, BorderLayout.WEST);
        south.add(tfInput, BorderLayout.CENTER);
        south.add(btnSend, BorderLayout.EAST);
        add(south, BorderLayout.SOUTH);

        setSize(800,600);
        setLocationRelativeTo(null);

        // Listeners
        btnNew.addActionListener(e -> openTab());
        btnFile.addActionListener(e -> sendFileCurrent());
        btnSend.addActionListener(e -> sendTextCurrent());
        tfInput.addActionListener(e -> sendTextCurrent());
    }

    /** Añade una nueva pestaña de chat usando los datos proporcionados */
    public void addChat(LoginData data) {
        ConversationPanel conv = new ConversationPanel(data);
        tabs.addTab(data.getUsername(), conv);
        tabs.setSelectedComponent(conv);
    }

    private void openTab() {
        LoginData data = LoginDialog.showDialog(this);
        if (data != null) {
            addChat(data);
        }
    }

    private ConversationPanel getCurrent() {
        Component c = tabs.getSelectedComponent();
        return (c instanceof ConversationPanel) ? (ConversationPanel)c : null;
    }

    private void sendTextCurrent() {
        ConversationPanel conv = getCurrent();
        if (conv != null) {
            String text = tfInput.getText().trim();
            if (!text.isEmpty()) conv.sendText(text);
            tfInput.setText("");
        }
    }

    private void sendFileCurrent() {
        ConversationPanel conv = getCurrent();
        if (conv != null) conv.sendFile();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1) Pedimos login antes de todo
            LoginData data = LoginDialog.showDialog(null);
            if (data == null) {
                // Si cancela o cierra, salimos sin mostrar nada
                System.exit(0);
            }
            // 2) Creamos la ventana y la primera pestaña
            ChatWindow w = new ChatWindow();
            w.addChat(data);
            w.setVisible(true);
        });
    }
}
