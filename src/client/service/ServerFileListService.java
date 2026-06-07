package client.service;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerFileListService {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9999;
    
    public List<String> getArchiveList() throws IOException {
        System.out.println("Getting archive list from server...");
        return sendCommandAndGetList("LIST_ARCHIVES");
    }
    
    public List<String> getFolderList() throws IOException {
        System.out.println("Getting folder list from server...");
        return sendCommandAndGetList("LIST_FOLDERS");
    }
    
    public void downloadFile(String fileName, File saveTo, String fileType) throws IOException {
        System.out.println("Downloading: " + fileName);
        
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            
            if ("ARCHIVE".equals(fileType)) {
                dos.writeUTF("DOWNLOAD_ARCHIVE");
            } else {
                dos.writeUTF("DOWNLOAD_FOLDER");
            }
            dos.writeUTF(fileName);
            dos.flush();
            
            long fileSize = dis.readLong();
            
            if (fileSize == -1) {
                throw new IOException("File not found on server: " + fileName);
            }
            
            System.out.println("File size: " + fileSize + " bytes");
            
            try (FileOutputStream fos = new FileOutputStream(saveTo)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                long received = 0;
                
                while (received < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    received += bytesRead;
                }
            }
            
            System.out.println("Download complete: " + saveTo.getAbsolutePath());
        }
    }
    
    private List<String> sendCommandAndGetList(String command) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            
            dos.writeUTF(command);
            dos.flush();
            
            int count = dis.readInt();
            System.out.println("Server returned " + count + " items");
            
            List<String> files = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                files.add(dis.readUTF());
            }
            
            return files;
        }
    }
}