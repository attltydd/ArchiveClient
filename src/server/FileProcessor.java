package server;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileProcessor {
    
    private final String storageDir;
    
    public FileProcessor(String storageDir) {
        this.storageDir = storageDir;
    }
    
    public String saveFolderArchive(File receivedZipFile, String originalName) throws IOException {
        System.out.println("  Saving folder archive...");
        
        String zipName = originalName;
        if (zipName.endsWith(".zip")) {
            zipName = zipName.substring(0, zipName.length() - 4);
        }
        zipName = zipName + "_archived.zip";
        
        File finalZipFile = new File(storageDir, zipName);
        
        try (FileInputStream fis = new FileInputStream(receivedZipFile);
             FileOutputStream fos = new FileOutputStream(finalZipFile)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        
        System.out.println("  Saved: " + finalZipFile.getName() + " (" + finalZipFile.length() + " bytes)");
        receivedZipFile.delete();
        
        return "SUCCESS: Folder saved as " + finalZipFile.getName();
    }
    
    public String extractAndSaveArchive(File receivedZipFile, String originalName) throws IOException {
        System.out.println("  Extracting archive...");
        
        String folderName = originalName;
        if (folderName.endsWith(".zip")) {
            folderName = folderName.substring(0, folderName.length() - 4);
        }
        folderName = folderName + "_extracted";
        
        File extractFolder = new File(storageDir, folderName);
        
        extractZipToFolder(receivedZipFile, extractFolder);
        
        int fileCount = countFiles(extractFolder);
        System.out.println("  Extracted: " + extractFolder.getName() + " (" + fileCount + " files)");
        
        receivedZipFile.delete();
        
        return "SUCCESS: Archive extracted to " + extractFolder.getName() + " (" + fileCount + " files)";
    }
    
    
    public void zipFolderToFile(File sourceFolder, File outputZipFile) throws IOException {
        System.out.println("  Zipping folder: " + sourceFolder.getName());
        
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new FileOutputStream(outputZipFile))) {
            zipDirectoryRecursive(sourceFolder, sourceFolder.getName(), zos);
        }
        
        System.out.println("  ZIP created: " + outputZipFile.getName() + " (" + outputZipFile.length() + " bytes)");
    }
    
    private void zipDirectoryRecursive(File dir, String parentPath, java.util.zip.ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        for (File file : files) {
            if (file.isDirectory()) {
                String entryPath = parentPath + "/" + file.getName() + "/";
                zos.putNextEntry(new ZipEntry(entryPath));
                zos.closeEntry();
                zipDirectoryRecursive(file, entryPath, zos);
            } else {
                String entryPath = parentPath + "/" + file.getName();
                zos.putNextEntry(new ZipEntry(entryPath));
                
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                }
                zos.closeEntry();
            }
        }
    }
    
    private void extractZipToFolder(File zipFile, File targetFolder) throws IOException {
        if (!targetFolder.exists()) {
            targetFolder.mkdirs();
        }
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buffer = new byte[8192];
            
            while ((entry = zis.getNextEntry()) != null) {
                File outputFile = new File(targetFolder, entry.getName());
                
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    File parentDir = outputFile.getParentFile();
                    if (parentDir != null && !parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                    
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }
    
    private int countFiles(File folder) {
        int count = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    count++;
                } else if (file.isDirectory()) {
                    count += countFiles(file);
                }
            }
        }
        return count;
    }
}