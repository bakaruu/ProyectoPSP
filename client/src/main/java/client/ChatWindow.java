package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * ChatWindow que gestiona múltiples conversaciones en pestañas con cierre.
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

    /**
     * Añade una nueva pestaña de chat usando los datos proporcionados,
     * con un botón X para cerrarla.
     */
    public void addChat(LoginData data) {
        ConversationPanel conv = new ConversationPanel(data);
        // Añadimos la pestaña y configuramos el header con cierre
        tabs.addTab(data.getUsername(), conv);
        int index = tabs.indexOfComponent(conv);

        // Header personalizado
        JPanel tabHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabHeader.setOpaque(false);
        JLabel lblTitle = new JLabel(" " + data.getUsername() + " ");
        tabHeader.add(lblTitle);

        JButton btnClose = new JButton(" X");
        btnClose.setFont(btnClose.getFont().deriveFont(Font.PLAIN, 10f));
        btnClose.setMargin(new Insets(0, 4, 0, 4));
        btnClose.setBorder(null);
        btnClose.setFocusable(false);
        btnClose.setContentAreaFilled(false);
        btnClose.addActionListener(e -> tabs.remove(conv));
        tabHeader.add(btnClose);

        tabs.setTabComponentAt(index, tabHeader);
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
                System.exit(0);
            }
            // 2) Creamos la ventana y la primera pestaña
            ChatWindow w = new ChatWindow();
            w.addChat(data);
            w.setVisible(true);
        });
    }
}
