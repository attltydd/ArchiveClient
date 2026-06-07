package client.service;

import client.model.FileItem;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ArchiveService {

    public void archiveDirectory(File sourceDir, File outputZipFile) throws IOException {
        System.out.println("ARCHIVE START");
        System.out.println("Source folder: " + sourceDir.getAbsolutePath());
        System.out.println("Output ZIP: " + outputZipFile.getAbsolutePath());
        
        if (!sourceDir.exists()) {
            throw new IOException("Folder does not exist: " + sourceDir.getAbsolutePath());
        }
        
        File[] files = sourceDir.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("WARNING: Folder is empty!");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZipFile))) {
            }
            return;
        }
        
        System.out.println("Files in folder: " + files.length);
        for (File f : files) {
            System.out.println("  - " + f.getName() + (f.isDirectory() ? " (folder)" : " (file, " + f.length() + " bytes)"));
        }
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputZipFile))) {
            zipDirectory(sourceDir, "", zos);
        }
        
        System.out.println("ZIP created, size: " + outputZipFile.length() + " bytes");
        System.out.println("ARCHIVE END");
    }
    
    public void extractArchive(File zipFile, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outputFile = new File(targetDir, entry.getName());
                
                if (entry.isDirectory()) {
                    outputFile.mkdirs();
                } else {
                    outputFile.getParentFile().mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[8192];
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
    
    public List<FileItem> getArchiveContents(File zipFile) throws IOException {
        List<FileItem> contents = new ArrayList<>();
        
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                
                long size = 0;
                if (!entry.isDirectory()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = zis.read(buffer)) != -1) {
                        size += bytesRead;
                    }
                }
                
                String displayName = name;
                if (displayName.endsWith("/")) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                
                contents.add(new FileItem(displayName, size));
                zis.closeEntry();
            }
        }
        
        contents.sort((a, b) -> {
            boolean aIsDir = a.getName().endsWith("/");
            boolean bIsDir = b.getName().endsWith("/");
            if (aIsDir && !bIsDir) return -1;
            if (!aIsDir && bIsDir) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        
        return contents;
    }
    
    private void zipDirectory(File dir, String parentPath, ZipOutputStream zos) throws IOException {
        File[] files = dir.listFiles();
        if (files == null) return;
        
        System.out.println("  Processing folder: " + dir.getName() + ", parentPath='" + parentPath + "'");
        
        for (File file : files) {
            if (file.isDirectory()) {
                String entryPath = parentPath + file.getName() + "/";
                System.out.println("    Adding folder: " + entryPath);
                ZipEntry zipEntry = new ZipEntry(entryPath);
                zos.putNextEntry(zipEntry);
                zos.closeEntry();
                zipDirectory(file, entryPath, zos);
            } else {
                String entryPath = parentPath + file.getName();
                System.out.println("    Adding file: " + entryPath + " (size: " + file.length() + " bytes)");
                addToZip(file, zos, parentPath);
            }
        }
    }
    
    private void addToZip(File file, ZipOutputStream zos, String path) throws IOException {
        String entryName = path + file.getName();
        System.out.println("      addToZip: entryName=" + entryName);
        
        ZipEntry zipEntry = new ZipEntry(entryName);
        zipEntry.setSize(file.length());
        zos.putNextEntry(zipEntry);
        
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        
        zos.closeEntry();
        System.out.println("      File added: " + entryName);
    }
}