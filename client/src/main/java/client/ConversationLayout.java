package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Layout y utilidades para insertar texto, imagenes y botones,
 * con mensajes alineados estilo "WhatsApp" personalizado.
 */
public class ConversationLayout extends JPanel {
    private final JTextPane tpChat = new JTextPane();
    private final StyledDocument doc;
    private final LoginData data;

    public ConversationLayout(LoginData data) {
        super(new BorderLayout());
        this.data = data;
        tpChat.setEditable(false);
        doc = tpChat.getStyledDocument();
        add(new JScrollPane(tpChat), BorderLayout.CENTER);
    }

    public StyledDocument getDocument() {
        return doc;
    }

    public void insertCaret() {
        tpChat.setCaretPosition(doc.getLength());
    }

    // Métodos insertImage e insertFileIcon se mantienen sin cambios.
    public void insertImage(BufferedImage img, String name, byte[] bytes, boolean isLocal) throws BadLocationException {
        int max = 100;
        int w = img.getWidth(), h = img.getHeight();
        double ratio = Math.min((double) max / w, (double) max / h);
        int nw = (int) (w * ratio), nh = (int) (h * ratio);
        ImageIcon icon = new ImageIcon(img.getScaledInstance(nw, nh, Image.SCALE_SMOOTH));
        JLabel lbl = new JLabel(icon);
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                JFrame fr = new JFrame(name);
                fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                fr.add(new JScrollPane(new JLabel(new ImageIcon(img))), BorderLayout.CENTER);
                Dimension sc = Toolkit.getDefaultToolkit().getScreenSize();
                fr.setSize(sc.width - 50, sc.height - 50);
                fr.setLocationRelativeTo(null);
                fr.setVisible(true);
            }
        });
        tpChat.insertComponent(lbl);
        doc.insertString(doc.getLength(), "\n", null);
        if (!isLocal) {
            JButton btn = new JButton("Guardar");
            btn.addActionListener(e -> saveFile(name, bytes));
            tpChat.insertComponent(btn);
            doc.insertString(doc.getLength(), "\n", null);
        }
    }

    public void insertFileIcon(String name, byte[] bytes, boolean isLocal) throws BadLocationException {
        Icon icon = UIManager.getIcon("FileView.fileIcon");
        JLabel lbl = new JLabel(name, icon, JLabel.LEFT);
        tpChat.insertComponent(lbl);
        doc.insertString(doc.getLength(), "\n", null);
        if (!isLocal) {
            JButton btn = new JButton("Guardar");
            btn.addActionListener(e -> saveFile(name, bytes));
            tpChat.insertComponent(btn);
            doc.insertString(doc.getLength(), "\n", null);
        }
    }

    private void saveFile(String name, byte[] data) {
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Guardar archivo", FileDialog.SAVE);
        fd.setFile(name);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            try (FileOutputStream fos = new FileOutputStream(new File(dir, file))) {
                fos.write(data);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error guardando: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Inserta un mensaje de texto con avatar, alineado y coloreado según quien lo envía.
     *
     * @param user   El nick de quien envía.
     * @param text   El contenido.
     * @param avatar Su avatar en ImageIcon.
     * @param isOwn  true si user.equals(data.getUsername()).
     */
    public void insertMessage(String user, String text, ImageIcon avatar, boolean isOwn) throws BadLocationException {
        JPanel bubble = new JPanel(new BorderLayout(5, 0));
        bubble.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        // FONDO: entrantes verdes, propios grises
        Color bg = isOwn ? new Color(240, 240, 240) : new Color(220, 248, 198);
        bubble.setBackground(bg);

        // Etiqueta de texto: si es propio, solo el texto y alineado a la derecha
        JLabel lbl;
        if (isOwn) {
            lbl = new JLabel(text);
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        } else {
            lbl = new JLabel("<html><b>" + user + ":</b> " + text + "</html>");
            lbl.setHorizontalAlignment(SwingConstants.LEFT);
        }
        lbl.setOpaque(false);

        JLabel pic = new JLabel(avatar);
        pic.setOpaque(false);

        if (isOwn) {
            // Mensaje propio: texto a la izquierda, avatar a la derecha
            bubble.add(lbl, BorderLayout.CENTER);
            bubble.add(pic, BorderLayout.EAST);
        } else {
            // Mensaje entrante: avatar a la izquierda, texto a la derecha
            bubble.add(pic, BorderLayout.WEST);
            bubble.add(lbl, BorderLayout.CENTER);
        }

        tpChat.insertComponent(bubble);
        doc.insertString(doc.getLength(), "\n", null);
        insertCaret();
    }

    /**
     * Inserta mensaje de sistema (sin avatar).
     */
    public void insertSystemMessage(String msg) throws BadLocationException {
        doc.insertString(doc.getLength(), "[ " + msg + " ]\n", null);
        insertCaret();
    }
}
