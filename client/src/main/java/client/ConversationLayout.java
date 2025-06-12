// src/main/java/client/ConversationLayout.java
package client;

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
import java.util.Base64;
import javax.imageio.ImageIO;

/**
 * Layout y utilidades para insertar texto, imagenes y botones.
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

    public void insertImage(BufferedImage img, String name, byte[] bytes, boolean isLocal) throws BadLocationException {
        int max = 100;
        int w = img.getWidth(), h = img.getHeight();
        double ratio = Math.min((double)max / w, (double)max / h);
        int nw = (int)(w * ratio), nh = (int)(h * ratio);
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
                fr.setSize(sc.width-50, sc.height-50);
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
        FileDialog fd = new FileDialog((Frame)SwingUtilities.getWindowAncestor(this), "Guardar archivo", FileDialog.SAVE);
        fd.setFile(name);
        fd.setVisible(true);
        String dir = fd.getDirectory();
        String file = fd.getFile();
        if (dir != null && file != null) {
            try (FileOutputStream fos = new FileOutputStream(new File(dir,file))) {
                fos.write(data);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error guardando: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
