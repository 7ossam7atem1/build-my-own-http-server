package com.http.server;

import com.http.server.handler.ConnectionHandler;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpServer {
    private static final int DEFAULT_PORT = 4221;
    private static final int THREAD_POOL_SIZE = 10;
    
    private final int port;
    private final String directory;
    private final ExecutorService threadPool;
    private ServerSocket serverSocket;
    private boolean isRunning;

    public HttpServer(int port, String directory) {
        this.port = port;
        this.directory = directory;
        this.threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.isRunning = false;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        serverSocket.setReuseAddress(true);
        isRunning = true;
        
        System.out.println("Server starting on port " + port + " with directory: " + directory);
        System.out.println("Server ready for connections...");

        while (isRunning) {
            try {
                var clientSocket = serverSocket.accept();
                threadPool.execute(() -> new ConnectionHandler(clientSocket, directory).handle());
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Server error: " + e.getMessage());
                }
            }
        }
    }

    public void stop() {
        isRunning = false;
        threadPool.shutdown();
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing server socket: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        String directory = ".";

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--directory") && i + 1 < args.length) {
                directory = args[i + 1];
                i++;
            } else if (args[i].startsWith("--directory=")) {
                directory = args[i].substring("--directory=".length());
            } else {
                try {
                    port = Integer.parseInt(args[i]);
                } catch (NumberFormatException e) {
                    System.err.println("Warning: Unrecognized argument: " + args[i]);
                }
            }
        }

        var server = new HttpServer(port, directory);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }
} 