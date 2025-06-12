package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

/**
 * Diálogo de login que además carga/guarda la caché de conexiones previas.
 */
public class LoginDialog extends JDialog {
    private static final int AVATAR_SIZE = 60;

    // Historial de conexiones: cada item es "nick|ip|puerto"
    private final JComboBox<String> cbHistory = new JComboBox<>();
    private final JTextField tfNick    = new JTextField(15);
    private final JButton    btnAvatar = new JButton("Seleccionar...");
    private final JLabel     lblPreview = new JLabel();
    private final JTextField tfIp      = new JTextField("127.0.0.1", 15);
    private final JTextField tfPort    = new JTextField("12345", 6);

    private final JButton btnConnect = new JButton("Conectar");
    private final JButton btnCancel  = new JButton("Cancelar");

    private ImageIcon chosenAvatar;
    private LoginData result;

    private LoginDialog(Frame owner) {
        super(owner, "Login ChatApp", true);
        loadCache();
        initUI();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    /** Carga ~/.chatapp_cache y llena el combo */
    private void loadCache() {
        List<String[]> entries = CacheManager.load();
        cbHistory.addItem("Nuevo usuario");
        for (String[] e : entries) {
            cbHistory.addItem(e[0] + "|" + e[1] + "|" + e[2]);
        }
        cbHistory.addActionListener(ev -> {
            String sel = (String) cbHistory.getSelectedItem();
            if (sel != null && !sel.equals("Nuevo usuario")) {
                String[] p = sel.split("\\|");
                tfNick.setText(p[0]);
                tfIp.setText(p[1]);
                tfPort.setText(p[2]);
            } else {
                tfNick.setText("");
            }
        });
    }

    private void initUI() {
        // Preview avatar
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

        // Formulario con GridBag
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(8, 20, 4, 20);
        c.anchor = GridBagConstraints.WEST;

        // Historial
        c.gridx = 0; c.gridy = 0; c.gridwidth = 3; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(cbHistory, c);

        // Nick
        c.gridy = 1; c.gridx = 0; c.gridwidth = 1; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Nick:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0;
        form.add(tfNick, c);

        // Preview avatar
        c.gridx = 2; c.gridheight = 2; c.fill = GridBagConstraints.NONE; c.weightx = 0;
        form.add(lblPreview, c);
        c.gridheight = 1;

        // Botón avatar
        c.gridy = 2; c.gridx = 0; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Avatar:"), c);
        c.gridx = 1;
        form.add(btnAvatar, c);

        // IP destino
        c.gridy = 3; c.gridx = 0;
        form.add(new JLabel("IP destino:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(tfIp, c);

        // Puerto destino
        c.gridy = 4; c.gridx = 0; c.fill = GridBagConstraints.NONE;
        form.add(new JLabel("Puerto destino:"), c);
        c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        form.add(tfPort, c);

        // Botones inferior
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 20));
        buttons.add(btnConnect);
        buttons.add(btnCancel);

        buttons.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 9));

        btnConnect.addActionListener(e -> onConnect());
        btnCancel.addActionListener(e -> onCancel());

        // ESC = cancelar
        getRootPane().registerKeyboardAction(e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        setLayout(new BorderLayout(10,10));
        add(form, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
    }

    private void chooseAvatar() {
        FileDialog fd = new FileDialog(this, "Selecciona avatar", FileDialog.LOAD);
        fd.setFilenameFilter((dir, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".png") ||
                    lower.endsWith(".jpg") ||
                    lower.endsWith(".jpeg") ||
                    lower.endsWith(".gif");
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
        // Guardar en cache
        CacheManager.addAndSave(nick, ip, port);

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

    // Listener simplificado para DocumentEvents
    private abstract static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        public abstract void update();
        public void insertUpdate(javax.swing.event.DocumentEvent e) { update(); }
        public void removeUpdate(javax.swing.event.DocumentEvent e) { update(); }
        public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
    }
}
