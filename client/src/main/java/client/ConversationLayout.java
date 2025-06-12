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
import java.util.Base64;

/**
 * Layout y utilidades para insertar texto, imágenes y archivos,
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

    public void insertMessage(String user, String text, ImageIcon avatar, boolean isOwn) throws BadLocationException {
        JPanel bubble = createBubble(isOwn);
        JLabel lbl = new JLabel(isOwn ? text : "<html><b>" + user + ":</b> " + text + "</html>");
        lbl.setHorizontalAlignment(isOwn ? SwingConstants.RIGHT : SwingConstants.LEFT);
        lbl.setOpaque(false);
        bubble.add(lbl, BorderLayout.CENTER);
        bubble.add(new JLabel(avatar), isOwn ? BorderLayout.EAST : BorderLayout.WEST);
        commitComponent(bubble);
    }

    public void insertImageMessage(String user, BufferedImage img, String name, byte[] bytes, ImageIcon avatar, boolean isOwn) throws BadLocationException {
        int max = 100;
        int w = img.getWidth(), h = img.getHeight();
        double ratio = Math.min((double) max / w, (double) max / h);
        ImageIcon thumb = new ImageIcon(img.getScaledInstance((int) (w * ratio), (int) (h * ratio), Image.SCALE_SMOOTH));
        JLabel pic = new JLabel(thumb);
        pic.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pic.addMouseListener(new MouseAdapter() {
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
        JButton btn = new JButton("Guardar");
        btn.setOpaque(false);
        btn.addActionListener(e -> saveFile(name, bytes));

        JPanel content = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 0));
        content.setOpaque(false);
        content.add(pic);
        if (!isOwn) content.add(btn);

        JPanel bubble = createBubble(isOwn);
        if (isOwn) bubble.add(new JLabel(avatar), BorderLayout.EAST);
        bubble.add(content, BorderLayout.CENTER);
        if (!isOwn) bubble.add(new JLabel(avatar), BorderLayout.WEST);
        commitComponent(bubble);
    }

    public void insertFileMessage(String user, String filename, byte[] bytes, ImageIcon avatar, boolean isOwn) throws BadLocationException {
        JLabel fileIcon = new JLabel(filename, UIManager.getIcon("FileView.fileIcon"), JLabel.LEFT);
        fileIcon.setOpaque(false);
        JButton btn = new JButton("Guardar");
        btn.setOpaque(false);
        btn.addActionListener(e -> saveFile(filename, bytes));

        JPanel content = new JPanel(new FlowLayout(isOwn ? FlowLayout.RIGHT : FlowLayout.LEFT, 5, 0));
        content.setOpaque(false);
        content.add(fileIcon);
        if (!isOwn) content.add(btn);

        JPanel bubble = createBubble(isOwn);
        if (isOwn) bubble.add(new JLabel(avatar), BorderLayout.EAST);
        bubble.add(content, BorderLayout.CENTER);
        if (!isOwn) bubble.add(new JLabel(avatar), BorderLayout.WEST);
        commitComponent(bubble);
    }

    public void insertSystemMessage(String msg) throws BadLocationException {
        doc.insertString(doc.getLength(), "[ " + msg + " ]\n", null);
        insertCaret();
    }

    private JPanel createBubble(boolean isOwn) {
        JPanel bubble = new JPanel(new BorderLayout(5, 0));
        bubble.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        bubble.setBackground(isOwn ? new Color(240, 240, 240) : new Color(220, 248, 198));
        bubble.setOpaque(true);
        return bubble;
    }

    private void commitComponent(JComponent comp) throws BadLocationException {
        tpChat.insertComponent(comp);
        doc.insertString(doc.getLength(), "\n", null);
        insertCaret();
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
}
