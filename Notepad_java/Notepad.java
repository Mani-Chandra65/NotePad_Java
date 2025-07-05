import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;
import java.awt.event.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

public class Notepad extends JFrame {
    private JTabbedPane tabbedPane;
    private JLabel statusBar;
    private java.util.List<String> recentFiles = new ArrayList<>();
    private JComboBox<String> fontBox;
    private JComboBox<Integer> sizeBox;

    public Notepad() {
        setTitle("Notepad");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        tabbedPane = new JTabbedPane();
        add(tabbedPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        statusBar = new JLabel("Words: 0");

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontBox = new JComboBox<>(fonts);
        fontBox.setSelectedItem("Consolas");
        fontBox.addActionListener(e -> updateFont());

        sizeBox = new JComboBox<>(new Integer[]{12, 14, 16, 18, 20, 24, 28, 32});
        sizeBox.setSelectedItem(16);
        sizeBox.addActionListener(e -> updateFont());

        JPanel fontPanel = new JPanel();
        fontPanel.add(new JLabel("Font:"));
        fontPanel.add(fontBox);
        fontPanel.add(new JLabel("Size:"));
        fontPanel.add(sizeBox);

        bottomPanel.add(fontPanel, BorderLayout.WEST);
        bottomPanel.add(statusBar, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem exportPDFItem = new JMenuItem("Export as PDF");
        JMenuItem insertImageItem = new JMenuItem("Insert Image");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(exportPDFItem);
        fileMenu.add(insertImageItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem searchReplaceItem = new JMenuItem("Search/Replace");
        editMenu.add(searchReplaceItem);

        JMenu themeMenu = new JMenu("Theme");
        JMenuItem lightThemeItem = new JMenuItem("Light Theme");
        JMenuItem darkThemeItem = new JMenuItem("Dark Theme");
        themeMenu.add(lightThemeItem);
        themeMenu.add(darkThemeItem);

        lightThemeItem.addActionListener(e -> applyTheme(true));
        darkThemeItem.addActionListener(e -> applyTheme(false));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(themeMenu);
        setJMenuBar(menuBar);

        newItem.addActionListener(e -> addNewTab());
        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile());
        exportPDFItem.addActionListener(e -> exportToPDF());
        insertImageItem.addActionListener(e -> insertImage());
        searchReplaceItem.addActionListener(e -> searchReplace());

        addNewTab();
    }

    private void addNewTab() {
        JTextPane textPane = new JTextPane();
        textPane.setFont(new java.awt.Font("Consolas", java.awt.Font.PLAIN, 16));
        textPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JScrollPane scrollPane = new JScrollPane(textPane);
        tabbedPane.addTab("Untitled", scrollPane);
        tabbedPane.setSelectedComponent(scrollPane);

        UndoManager undoManager = new UndoManager();
        textPane.getDocument().addUndoableEditListener(undoManager);

        InputMap im = textPane.getInputMap();
        ActionMap am = textPane.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "Undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK), "Redo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "Save");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK), DefaultEditorKit.cutAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK), DefaultEditorKit.copyAction);
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), DefaultEditorKit.pasteAction);

        am.put("Undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) undoManager.undo();
            }
        });

        am.put("Redo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) undoManager.redo();
            }
        });

        am.put("Save", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                saveFile();
            }
        });

        textPane.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateWordCount(textPane); }
            public void removeUpdate(DocumentEvent e) { updateWordCount(textPane); }
            public void changedUpdate(DocumentEvent e) { updateWordCount(textPane); }
        });

        new javax.swing.Timer(30000, e -> saveFile()).start();
    }

    private void updateFont() {
        JTextPane textPane = getCurrentTextPane();
        if (textPane != null) {
            String fontName = (String) fontBox.getSelectedItem();
            int fontSize = (Integer) sizeBox.getSelectedItem();
            textPane.setFont(new java.awt.Font(fontName, java.awt.Font.PLAIN, fontSize));
        }
    }

    private void updateWordCount(JTextPane textPane) {
        String text = textPane.getText().trim();
        int words = text.isEmpty() ? 0 : text.split("\\s+").length;
        statusBar.setText("Words: " + words);
    }

    private JTextPane getCurrentTextPane() {
        JScrollPane scrollPane = (JScrollPane) tabbedPane.getSelectedComponent();
        if (scrollPane != null) {
            JViewport vp = scrollPane.getViewport();
            return (JTextPane) vp.getView();
        }
        return null;
    }

    private void openFile() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                JTextPane textPane = new JTextPane();
                textPane.read(reader, null);
                reader.close();

                JScrollPane scrollPane = new JScrollPane(textPane);
                tabbedPane.addTab(file.getName(), scrollPane);
                tabbedPane.setSelectedComponent(scrollPane);
                recentFiles.add(file.getAbsolutePath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void saveFile() {
        JTextPane textPane = getCurrentTextPane();
        if (textPane == null) return;

        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                textPane.write(writer);
                writer.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void exportToPDF() {
        JTextPane textPane = getCurrentTextPane();
        if (textPane == null) return;

        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                Document doc = new Document();
                PdfWriter.getInstance(doc, new FileOutputStream(file));
                doc.open();
                doc.add(new Paragraph(textPane.getText()));
                doc.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void insertImage() {
        JTextPane textPane = getCurrentTextPane();
        if (textPane == null) return;

        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                textPane.insertIcon(new ImageIcon(file.getAbsolutePath()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private void searchReplace() {
        JTextPane textPane = getCurrentTextPane();
        if (textPane == null) return;

        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField searchField = new JTextField();
        JTextField replaceField = new JTextField();
        panel.add(new JLabel("Find:"));
        panel.add(searchField);
        panel.add(new JLabel("Replace with:"));
        panel.add(replaceField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Search and Replace", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String search = searchField.getText();
            String replace = replaceField.getText();
            textPane.setText(textPane.getText().replaceAll(search, replace));
        }
    }

    private void applyTheme(boolean isLight) {
        Color navbarBg = isLight ? new Color(0x0047AB) : new Color(0xFFB853);
        Color navbarFg = isLight ? new Color(0x08E8DE) : new Color(0x472A00);
        Color bodyBg = isLight ? new Color(0x4A5D23) : new Color(0xFFFFFF);
        Color bodyFg = isLight ? new Color(0xF2F3F4) : new Color(0x000000);

        JMenuBar menuBar = getJMenuBar();
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            menu.setBackground(navbarBg);
            menu.setForeground(navbarFg);
            for (Component c : menu.getMenuComponents()) {
                if (c instanceof JMenuItem) {
                    c.setBackground(navbarBg);
                    c.setForeground(navbarFg);
                }
            }
        }

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            JScrollPane scrollPane = (JScrollPane) tabbedPane.getComponentAt(i);
            JTextPane textPane = (JTextPane) scrollPane.getViewport().getView();
            textPane.setBackground(bodyBg);
            textPane.setForeground(bodyFg);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Notepad().setVisible(true));
    }
}