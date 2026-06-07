package server;

import java.io.*;
import java.net.Socket;

public class ServerHandler implements Runnable {
    
    private final Socket clientSocket;
    private final String storageDir;
    private DataInputStream dis;
    private DataOutputStream dos;
    
    public ServerHandler(Socket socket, String storageDir) {
        this.clientSocket = socket;
        this.storageDir = storageDir;
    }
    
    @Override
    public void run() {
        try {
            dis = new DataInputStream(clientSocket.getInputStream());
            dos = new DataOutputStream(clientSocket.getOutputStream());
            
            String command = dis.readUTF();
            System.out.println("📨 Received command: " + command);
            
            switch (command) {
                case "FOLDER":
                    handleFolderUpload();
                    break;
                case "ARCHIVE":
                    handleArchiveUpload();
                    break;
                case "LIST_ARCHIVES":
                    sendFileList(".zip");
                    break;
                case "LIST_FOLDERS":
                    sendFileList("_extracted");
                    break;
                case "DOWNLOAD_ARCHIVE":
                    downloadArchive();
                    break;
                case "DOWNLOAD_FOLDER":
                    downloadFolder();
                    break;
                default:
                    dos.writeUTF("ERROR: Unknown command");
                    dos.flush();
            }
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void handleFolderUpload() throws IOException {
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        
        System.out.println("📁 Folder received (as ZIP): " + fileName);
        System.out.println("📊 Size: " + fileSize + " bytes");
        
        File receivedFile = receiveFile(fileName, fileSize);
        System.out.println("✅ File received");
        
        FileProcessor processor = new FileProcessor(storageDir);
        
      
        String extractResult = processor.extractAndSaveArchive(receivedFile, fileName);
        System.out.println("  📂 Extraction: " + extractResult);
        
       
        String folderName = fileName;
        if (folderName.endsWith(".zip")) {
            folderName = folderName.substring(0, folderName.length() - 4);
        }
        String extractedFolderName = folderName + "_extracted";
        File extractedFolder = new File(storageDir, extractedFolderName);
        
        String result;
        if (extractedFolder.exists() && extractedFolder.isDirectory()) {
            String archiveName = folderName + "_archived.zip";
            File archivedZip = new File(storageDir, archiveName);
            
            System.out.println("  📦 Archiving: " + extractedFolderName + " → " + archiveName);
            processor.zipFolderToFile(extractedFolder, archivedZip);
            System.out.println("  ✅ Archive created: " + archivedZip.getName() + " (" + archivedZip.length() + " bytes)");
            
           
            System.out.println("  🗑️ Deleting temporary folder: " + extractedFolderName);
            deleteDirectory(extractedFolder);
            
            result = "SUCCESS: Folder unpacked, archived to " + archiveName + ", temporary folder deleted";
            dos.writeUTF(result);
        } else {
            result = "SUCCESS but archive creation failed: " + extractResult;
            dos.writeUTF(result);
        }
        
        dos.flush();
        System.out.println("📨 Response sent\n");
    }
    
    private void handleArchiveUpload() throws IOException {
        String fileName = dis.readUTF();
        long fileSize = dis.readLong();
        
        System.out.println("📦 Archive: " + fileName);
        System.out.println("📊 Size: " + fileSize + " bytes");
        
        File receivedFile = receiveFile(fileName, fileSize);
        System.out.println("✅ File received");
        
        FileProcessor processor = new FileProcessor(storageDir);
        
      
        String result = processor.extractAndSaveArchive(receivedFile, fileName);
        
        dos.writeUTF(result);
        dos.flush();
        System.out.println("📨 Response sent\n");
    }
    
    private void sendFileList(String suffix) throws IOException {
        File storage = new File(storageDir);
        
        File[] allFiles = storage.listFiles();
        if (allFiles != null && allFiles.length > 0) {
            System.out.println("  Files in storage:");
            for (File f : allFiles) {
                System.out.println("    - " + f.getName());
            }
        }
        
        File[] files = storage.listFiles((dir, name) -> name.endsWith(suffix));
        
        if (files == null || files.length == 0) {
            System.out.println("  No files with suffix: " + suffix);
            dos.writeInt(0);
            dos.flush();
            return;
        }
        
        System.out.println("  Sending " + files.length + " files with suffix: " + suffix);
        dos.writeInt(files.length);
        for (File file : files) {
            System.out.println("    - " + file.getName());
            dos.writeUTF(file.getName());
        }
        dos.flush();
    }
    
    private void downloadArchive() throws IOException {
        String fileName = dis.readUTF();
        System.out.println("📥 Download archive: " + fileName);
        
        File targetFile = new File(storageDir, fileName);
        
        if (!targetFile.exists()) {
            System.out.println("  File not found");
            dos.writeLong(-1);
            dos.flush();
            return;
        }
        
        System.out.println("  Sending: " + targetFile.getName() + " (" + targetFile.length() + " bytes)");
        dos.writeLong(targetFile.length());
        
        try (FileInputStream fis = new FileInputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
        dos.flush();
        
        System.out.println("✅ Archive sent\n");
    }
    
    private void downloadFolder() throws IOException {
        String folderName = dis.readUTF();
        System.out.println("📥 Download folder: " + folderName);
        
        File sourceFolder = new File(storageDir, folderName);
        
        if (!sourceFolder.exists() || !sourceFolder.isDirectory()) {
            System.out.println("  Folder not found: " + folderName);
            dos.writeLong(-1);
            dos.flush();
            return;
        }
        
        File tempZip = new File(storageDir, "temp_download_" + System.currentTimeMillis() + ".zip");
        FileProcessor processor = new FileProcessor(storageDir);
        processor.zipFolderToFile(sourceFolder, tempZip);
        
        System.out.println("  Sending ZIP: " + tempZip.getName() + " (" + tempZip.length() + " bytes)");
        dos.writeLong(tempZip.length());
        
        try (FileInputStream fis = new FileInputStream(tempZip)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
            }
        }
        dos.flush();
        
        tempZip.delete();
        
        System.out.println("✅ Folder zipped and sent\n");
    }
    
    private File receiveFile(String fileName, long fileSize) throws IOException {
        File tempFile = new File(storageDir, "temp_" + System.currentTimeMillis() + "_" + fileName);
        
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            long received = 0;
            int bytesRead;
            int lastProgress = 0;
            
            while (received < fileSize) {
                bytesRead = dis.read(buffer);
                if (bytesRead == -1) break;
                
                fos.write(buffer, 0, bytesRead);
                received += bytesRead;
                
                int progress = (int) (received * 100 / fileSize);
                if (progress >= lastProgress + 10 && progress < 100) {
                    System.out.println("  Progress: " + progress + "%");
                    lastProgress = progress;
                }
            }
        }
        
        return tempFile;
    }
    
    private void deleteDirectory(File dir) {
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            dir.delete();
            System.out.println("    Deleted: " + dir.getAbsolutePath());
        }
    }
}