
package client.ui;

import client.service.ServerFileListService;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ServerFileBrowserDialog extends JDialog {
    
    private final JList<String> fileList;
    private final DefaultListModel<String> listModel;
    private final String fileType;
    private String selectedFile = null;
    private boolean confirmed = false;
    private JButton downloadButton;
    
    public ServerFileBrowserDialog(JFrame parent, String title, String fileType) {
        super(parent, title, true);
        this.fileType = fileType;
        
        setSize(500, 400);
        setLocationRelativeTo(parent);
        
        listModel = new DefaultListModel<>();
        fileList = new JList<>(listModel);
        fileList.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(fileList);
        
        downloadButton = new JButton("Download Selected");
        JButton cancelButton = new JButton("Cancel");
        JButton refreshButton = new JButton("Refresh");
        
       
        downloadButton.setEnabled(false);
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(refreshButton);
        buttonPanel.add(downloadButton);
        buttonPanel.add(cancelButton);
        
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        loadFileList();
        
        downloadButton.addActionListener(e -> {
            String selected = fileList.getSelectedValue();
            if (selected != null && !selected.equals("(no files)") && !selected.equals("Loading...")) {
                selectedFile = selected;
                confirmed = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Please select a file to download", 
                    "Error", 
                    JOptionPane.WARNING_MESSAGE);
            }
        });
        
        cancelButton.addActionListener(e -> dispose());
        
        refreshButton.addActionListener(e -> {
            downloadButton.setEnabled(false);
            loadFileList();
        });
        
        fileList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    downloadButton.doClick();
                }
            }
        });
        
        setVisible(true);
    }
    
    private void loadFileList() {
        listModel.clear();
        listModel.addElement("Loading...");
        
        new Thread(() -> {
            try {
                ServerFileListService service = new ServerFileListService();
                List<String> files;
                
                if ("ARCHIVE".equals(fileType)) {
                    System.out.println("Requesting archive list from server...");
                    files = service.getArchiveList();
                } else {
                    System.out.println("Requesting folder list from server...");
                    files = service.getFolderList();
                }
                
                System.out.println("Received " + files.size() + " files");
                
                final List<String> finalFiles = files;
                
              
                finalFiles.sort(String.CASE_INSENSITIVE_ORDER);
                
                SwingUtilities.invokeLater(() -> {
                    listModel.clear();
                    if (finalFiles.isEmpty()) {
                        listModel.addElement("(no files)");
                        downloadButton.setEnabled(false);
                    } else {
                        for (String file : finalFiles) {
                            listModel.addElement(file);
                        }
                        downloadButton.setEnabled(true);
                    }
                });
            } catch (Exception ex) {
                System.err.println("Error loading list: " + ex.getMessage());
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    listModel.clear();
                    listModel.addElement("Error: " + ex.getMessage());
                    downloadButton.setEnabled(false);
                });
            }
        }).start();
    }
    
    public String getSelectedFile() {
        return confirmed ? selectedFile : null;
    }
}