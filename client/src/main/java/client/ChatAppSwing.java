package client;

import javax.swing.*;

/**
 * Clase principal del cliente Swing.
 */
public class ChatAppSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginDialog login = new LoginDialog(null);
            LoginData data = login.showDialog();
            if (data != null) {
                ChatWindow window = new ChatWindow(data);
                window.setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}
