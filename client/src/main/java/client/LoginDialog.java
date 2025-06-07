package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Diálogo para que el usuario introduzca nombre, IP y puerto.
 */
public class LoginDialog extends JDialog {
    private JTextField tfName = new JTextField(15);
    private JTextField tfIp   = new JTextField("127.0.0.1", 15);
    private JTextField tfPort = new JTextField("12345", 5);
    private LoginData loginData = null;

    public LoginDialog(Frame owner) {
        super(owner, "Conectar al chat", true);
        setLayout(new GridLayout(4,2,5,5));

        add(new JLabel("Nombre:")); add(tfName);
        add(new JLabel("Servidor IP:")); add(tfIp);
        add(new JLabel("Puerto:")); add(tfPort);

        JButton btnOk = new JButton("Conectar");
        btnOk.addActionListener((ActionEvent e) -> onConnect());
        add(btnOk);

        pack();
        setLocationRelativeTo(owner);
    }

    private void onConnect() {
        String name = tfName.getText().trim();
        String ip   = tfIp.getText().trim();
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Puerto inválido");
            return;
        }
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío");
            return;
        }
        loginData = new LoginData(name, ip, port);
        dispose();
    }

    /**
     * Muestra el diálogo y devuelve null si el usuario canceló,
     * o los datos de conexión si pulsó Conectar.
     */
    public LoginData showDialog() {
        setVisible(true);
        return loginData;
    }
}
