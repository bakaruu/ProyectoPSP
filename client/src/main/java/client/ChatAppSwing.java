package client;

import javax.swing.*;

/**
 * Clase principal del cliente Swing.
 */
public class ChatAppSwing {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginData login = LoginDialog.showDialog(null);
            if (login != null) {
                ChatWindow window = new ChatWindow(login);
                window.setVisible(true);
            } else {
                System.exit(0);
            }


        });
    }
}
