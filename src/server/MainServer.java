package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    
    private static final int PORT = 9999;
    private static final String STORAGE_DIR = "server_storage";
    
    public static void main(String[] args) {
        File storage = new File(STORAGE_DIR);
        if (!storage.exists()) {
            storage.mkdirs();
        }
        
        System.out.println("=== ARCHIVE SERVER ===");
        System.out.println("Started on port: " + PORT);
        System.out.println("Storage directory: " + storage.getAbsolutePath());
        System.out.println("Waiting for connections...\n");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("✅ Client connected: " + clientSocket.getInetAddress());
                
                ServerHandler handler = new ServerHandler(clientSocket, STORAGE_DIR);
                Thread thread = new Thread(handler);
                thread.start();
            }
            
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}