package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class LoginDialog extends JDialog {
    private static final int AVATAR_SIZE = 60;

    private final JTextField tfNick    = new JTextField(15);
    private final JButton    btnAvatar = new JButton("Seleccionar...");
    private final JLabel     lblPreview = new JLabel();
    private final JTextField tfIp      = new JTextField("127.0.0.1", 15);
    private final JTextField tfPort = new JTextField("12345", 6);

    private final JButton btnConnect = new JButton("Conectar");
    private final JButton btnCancel  = new JButton("Cancelar");

    private ImageIcon chosenAvatar;
    private LoginData result;

    private LoginDialog(Frame owner) {
        super(owner, "Login ChatApp", true);
        initUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void initUI() {
        // Preview cuadrado, solo una fila, bien pegado
        lblPreview.setPreferredSize(new Dimension(AVATAR_SIZE, AVATAR_SIZE));
        lblPreview.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        lblPreview.setOpaque(true);
        lblPreview.setBackground(Color.WHITE);

        btnAvatar.addActionListener(e -> chooseAvatar());

        btnConnect.setEnabled(false);
        tfNick.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override public void update() {
                btnConnect.setEnabled(!tfNick.getText().trim().isEmpty());
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(10, 28, 4, 20); // ¡menos separación vertical!
        c.anchor = GridBagConstraints.WEST;

        // Row 0: Nick
        c.gridx = 0; c.gridy = 0; c.gridwidth = 1; c.gridheight = 1; c.weightx = 0;
        form.add(new JLabel("Nick:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        form.add(tfNick, c);
        c.gridx = 2; c.gridheight = 2; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        // Para que preview quede justo al lado de avatar y centrado verticalmente (en filas 0 y 1)
        form.add(lblPreview, c);
        c.gridheight = 1;

        // Row 1: Avatar label y botón
        c.gridy = 1; c.gridx = 0; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Avatar:"), c);
        c.gridx = 1;
        form.add(btnAvatar, c);

        // Row 2: IP destino
        c.gridy = 2; c.gridx = 0;
        form.add(new JLabel("IP destino:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(tfIp, c);

        // Row 3: Puerto destino
        c.gridy = 3; c.gridx = 0; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Puerto destino:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(tfPort, c);

        // Panel de botones: Conectar | Cancelar (igual que tu imagen, Conectar primero)
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 20));
        buttons.add(btnConnect);
        buttons.add(btnCancel);

        btnConnect.addActionListener(e -> onConnect());
        btnCancel.addActionListener(e -> onCancel());

        // ESC = cancelar
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setLayout(new BorderLayout());
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private void chooseAvatar() {
        FileDialog fd = new FileDialog(this, "Selecciona avatar", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif");
        });
        fd.setVisible(true);
        String dir = fd.getDirectory(), file = fd.getFile();
        if (dir != null && file != null) {
            try {
                BufferedImage img = ImageIO.read(new File(dir, file));
                Image scaled = img.getScaledInstance(AVATAR_SIZE, AVATAR_SIZE, Image.SCALE_SMOOTH);
                chosenAvatar = new ImageIcon(scaled);
                lblPreview.setIcon(chosenAvatar);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error al cargar avatar: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onConnect() {
        String nick = tfNick.getText().trim();
        String ip   = tfIp.getText().trim();
        int port;
        try {
            port = Integer.parseInt(tfPort.getText().trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Puerto inválido.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (chosenAvatar == null) {
            BufferedImage empty = new BufferedImage(AVATAR_SIZE, AVATAR_SIZE, BufferedImage.TYPE_INT_ARGB);
            chosenAvatar = new ImageIcon(empty);
        }
        result = new LoginData(nick, chosenAvatar, ip, port);
        dispose();
    }

    private void onCancel() {
        result = null;
        dispose();
    }

    public static LoginData showDialog(Frame owner) {
        LoginDialog dlg = new LoginDialog(owner);
        dlg.setVisible(true);
        return dlg.result;
    }

    private abstract static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        public abstract void update();
        public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }
}
