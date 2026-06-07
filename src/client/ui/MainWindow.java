package client.ui;

import client.service.ArchiveClientService;
import client.service.ArchiveService;
import client.service.FileExplorerService;
import client.service.ServerFileListService;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MainWindow extends JFrame {

    private final JTable folderTable;
    private final JTable archiveTable;

    private final FileTableModel folderModel;
    private final FileTableModel archiveModel;

    private final JButton selectFolderButton;
    private final JButton selectArchiveButton;
    private final JButton sendFolderToServerButton;
    private final JButton sendArchiveToServerButton;
    private final JButton getArchivesFromServerButton;
    private final JButton getFoldersFromServerButton;

    private final FileExplorerService explorerService;
    private final ArchiveService archiveService;

    private File currentFolder;
    private File currentArchiveFile;

    public MainWindow() {
        explorerService = new FileExplorerService();
        archiveService = new ArchiveService();

        setTitle("ZIP_ARCHIVATOR_V1.0");
        setSize(1100, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        folderModel = new FileTableModel();
        archiveModel = new FileTableModel();

        folderTable = new JTable(folderModel);
        archiveTable = new JTable(archiveModel);

        selectFolderButton = new JButton("<html><center>📁 Выбрать папку и<br>посмотреть содержимое</center></html>");
        selectArchiveButton = new JButton("<html><center>📦 Выбрать архив и<br>посмотреть содержимое</center></html>");
        sendFolderToServerButton = new JButton("<html><center>📤 Архивировать папку и<br>сохранить архив на сервере</center></html>");
        sendArchiveToServerButton = new JButton("<html><center>📤 Распаковать архив и<br>сохранить папку на сервере</center></html>");
        getArchivesFromServerButton = new JButton("<html><center>📥 Получить сохраненную на сервере<br>папку распакованного архива</center></html>");
        getFoldersFromServerButton = new JButton("<html><center>📥 Получить сохраненный на сервере<br>архив запакованной папки</center></html>");

        initializeComponents();
        registerEvents();
    }

    private void initializeComponents() {
        JScrollPane leftPane = new JScrollPane(folderTable);
        leftPane.setBorder(BorderFactory.createTitledBorder("📁 Содержимое выбранной папки"));

        JScrollPane rightPane = new JScrollPane(archiveTable);
        rightPane.setBorder(BorderFactory.createTitledBorder("📦 Содержимое выбранного архива"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
        splitPane.setDividerLocation(480);

        JPanel folderPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        folderPanel.setBorder(BorderFactory.createTitledBorder("Операции над папками"));
        folderPanel.add(selectFolderButton);
        folderPanel.add(sendFolderToServerButton);
        folderPanel.add(getFoldersFromServerButton);  
        
        JPanel archivePanel = new JPanel(new GridLayout(3, 1, 5, 5));
        archivePanel.setBorder(BorderFactory.createTitledBorder("Операции над архивами"));
        archivePanel.add(selectArchiveButton);
        archivePanel.add(sendArchiveToServerButton);
        archivePanel.add(getArchivesFromServerButton);  
        
        JPanel mainButtonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        mainButtonPanel.add(folderPanel);
        mainButtonPanel.add(archivePanel);

        add(splitPane, BorderLayout.CENTER);
        add(mainButtonPanel, BorderLayout.SOUTH);
    }

    private void registerEvents() {
        selectFolderButton.addActionListener(e -> chooseFolder());
        selectArchiveButton.addActionListener(e -> chooseArchiveFile());
        sendFolderToServerButton.addActionListener(e -> sendCurrentFolderToServer());
        sendArchiveToServerButton.addActionListener(e -> sendCurrentArchiveToServer());
        getFoldersFromServerButton.addActionListener(e -> downloadFromServer("ARCHIVE"));     
        getArchivesFromServerButton.addActionListener(e -> downloadFromServer("FOLDER"));     
    }

    private void chooseFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select folder to send to server");

        int result = chooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            currentFolder = chooser.getSelectedFile();
            folderModel.setFiles(explorerService.loadDirectory(currentFolder));
            
            JOptionPane.showMessageDialog(this,
                "Selected folder: " + currentFolder.getName() +
                "\nFiles: " + folderModel.getRowCount(),
                "Folder Selected",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void chooseArchiveFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select ZIP archive to send to server");
        chooser.setFileFilter(new FileNameExtensionFilter("ZIP archives", "zip"));
        
        int result = chooser.showOpenDialog(this);
        
        if (result == JFileChooser.APPROVE_OPTION) {
            currentArchiveFile = chooser.getSelectedFile();
            
            try {
                archiveModel.setFiles(archiveService.getArchiveContents(currentArchiveFile));
                JOptionPane.showMessageDialog(this,
                    "Selected archive: " + currentArchiveFile.getName() +
                    "\nSize: " + currentArchiveFile.length() + " bytes",
                    "Archive Selected",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Error reading archive: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void sendCurrentFolderToServer() {
        if (currentFolder == null) {
            JOptionPane.showMessageDialog(this,
                "Please select a folder first!",
                "Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Thread sendThread = new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)));
                
                ArchiveClientService clientService = new ArchiveClientService();
                String result = clientService.sendFolderToServer(currentFolder);
                
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(MainWindow.this,
                        "✅ Server response:\n" + result,
                        "Send Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(MainWindow.this,
                        "❌ Error: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
        sendThread.start();
    }
    
    private void sendCurrentArchiveToServer() {
        if (currentArchiveFile == null) {
            JOptionPane.showMessageDialog(this,
                "Please select an archive first!",
                "Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        Thread sendThread = new Thread(() -> {
            try {
                SwingUtilities.invokeLater(() -> setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)));
                
                ArchiveClientService clientService = new ArchiveClientService();
                String result = clientService.sendArchiveToServer(currentArchiveFile);
                
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(MainWindow.this,
                        "✅ Server response:\n" + result,
                        "Send Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    setCursor(Cursor.getDefaultCursor());
                    JOptionPane.showMessageDialog(MainWindow.this,
                        "❌ Error: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                });
            }
        });
        sendThread.start();
    }
    
    private void downloadFromServer(String fileType) {
        String title = fileType.equals("ARCHIVE") ? "Select folder to download (result of extraction)" : "Select archive to download (result of archiving)";
        String dialogType = fileType.equals("ARCHIVE") ? "ARCHIVE" : "FOLDER";
        
        ServerFileBrowserDialog dialog = new ServerFileBrowserDialog(this, title, dialogType);
        dialog.setVisible(true);
        
        String selectedFile = dialog.getSelectedFile();
        if (selectedFile == null) return;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save As");
        
        if (fileType.equals("FOLDER")) {
            String zipName = selectedFile.replace("_extracted", "") + ".zip";
            chooser.setSelectedFile(new File(zipName));
        } else {
            chooser.setSelectedFile(new File(selectedFile));
        }
        
        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File saveTo = chooser.getSelectedFile();
            
            if (fileType.equals("FOLDER") && !saveTo.getName().endsWith(".zip")) {
                saveTo = new File(saveTo.getAbsolutePath() + ".zip");
            }
            
            final File finalSaveTo = saveTo;
            final String finalSelectedFile = selectedFile;
            final String finalFileType = fileType;
            
            Thread downloadThread = new Thread(() -> {
                try {
                    SwingUtilities.invokeLater(() -> setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)));
                    
                    ServerFileListService service = new ServerFileListService();
                    service.downloadFile(finalSelectedFile, finalSaveTo, finalFileType);
                    
                    if (finalFileType.equals("FOLDER") && finalSaveTo.getName().endsWith(".zip")) {
                        try {
                            String extractedPath = finalSaveTo.getAbsolutePath().replace(".zip", "_extracted");
                            File extractedDir = new File(extractedPath);
                            archiveService.extractArchive(finalSaveTo, extractedDir);
                            finalSaveTo.delete();
                            
                            SwingUtilities.invokeLater(() -> {
                                setCursor(Cursor.getDefaultCursor());
                                JOptionPane.showMessageDialog(MainWindow.this,
                                    "📂 Создана папка:\n" + extractedDir.getAbsolutePath(),
                                    "Готово",
                                    JOptionPane.INFORMATION_MESSAGE);
                            });
                        } catch (IOException ex) {
                            SwingUtilities.invokeLater(() -> {
                                setCursor(Cursor.getDefaultCursor());
                                JOptionPane.showMessageDialog(MainWindow.this,
                                    "❌ Ошибка при распаковке:\n" + ex.getMessage(),
                                    "Ошибка",
                                    JOptionPane.ERROR_MESSAGE);
                            });
                        }
                    } else {
                        SwingUtilities.invokeLater(() -> {
                            setCursor(Cursor.getDefaultCursor());
                            JOptionPane.showMessageDialog(MainWindow.this,
                                "✅ Файл сохранён:\n" + finalSaveTo.getAbsolutePath(),
                                "Готово",
                                JOptionPane.INFORMATION_MESSAGE);
                        });
                    }
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> {
                        setCursor(Cursor.getDefaultCursor());
                        JOptionPane.showMessageDialog(MainWindow.this,
                            "❌ Ошибка: " + ex.getMessage(),
                            "Ошибка",
                            JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
            downloadThread.start();
        }
    }
}