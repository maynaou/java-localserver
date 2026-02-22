package com.javaserver.server;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
public class Server {
    private static final int PORT = 8080;

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Handle the client connection in a separate thread
                
            }
        } catch (IOException e) {
            e.printStackTrace();    
        }
    }
}