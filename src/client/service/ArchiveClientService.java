package client.service;

import java.io.*;
import java.net.Socket;

public class ArchiveClientService {
    
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9999;
   
    public String sendFolderToServer(File folder) throws IOException {
        System.out.println("===== SENDING FOLDER TO SERVER =====");
        System.out.println("Folder: " + folder.getAbsolutePath());
        File tempZip = File.createTempFile("send_folder_", ".zip");
        tempZip.deleteOnExit();
        ArchiveService archiveService = new ArchiveService();
        archiveService.archiveDirectory(folder, tempZip);
        System.out.println("Created ZIP size: " + tempZip.length() + " bytes");
        return sendFileToServer(tempZip, "FOLDER", folder.getName());
    }
    
    public String sendArchiveToServer(File archive) throws IOException {
        System.out.println("Sending archive to server: " + archive.getName());
        return sendFileToServer(archive, "ARCHIVE", archive.getName());
    }
    
    private String sendFileToServer(File file, String operationType, String originalName) throws IOException {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
             DataInputStream dis = new DataInputStream(socket.getInputStream());
             FileInputStream fis = new FileInputStream(file)) {
            
            System.out.println("Connecting to server...");
            dos.writeUTF(operationType);
            dos.writeUTF(originalName);
            long fileSize = file.length();
            dos.writeLong(fileSize);
            System.out.println("Sending file (size: " + fileSize + " bytes)...");
            byte[] buffer = new byte[8192];
            int bytesRead;
            long sent = 0;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                sent += bytesRead;
            }
            
            dos.flush();
            System.out.println("File sent, waiting for response...");
            String response = dis.readUTF();
            System.out.println("Server response: " + response);
            return response;
        }
    }
}