import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int DEFAULT_PORT = 4221;
    private static String directory = ".";

    public static void main(String[] args) {
        int port = DEFAULT_PORT;

        // Parse command line arguments
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--directory") && i + 1 < args.length) {
                directory = args[i + 1];
                i++; // Skip the next argument as we've already processed it
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

        System.out.println("Server starting on port " + port + " with directory: " + directory);

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server ready for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleRequest(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    private static void handleRequest(Socket clientSocket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
        ) {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(writer, 400, "Bad Request");
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendErrorResponse(writer, 400, "Bad Request");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            String userAgent = null;
            int contentLength = 0;

            // Read headers
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.startsWith("User-Agent: ")) {
                    userAgent = headerLine.substring(12);
                } else if (headerLine.startsWith("Content-Length: ")) {
                    contentLength = Integer.parseInt(headerLine.substring(16).trim());
                }
            }

            // Handle different endpoints
            if (path.equals("/")) {
                sendResponse(writer, 200, "OK", null);
            } else if (path.startsWith("/echo/")) {
                handleEchoRequest(writer, path);
            } else if (path.equals("/user-agent")) {
                handleUserAgentRequest(writer, userAgent);
            } else if (path.startsWith("/files/")) {
                if (method.equals("GET")) {
                    handleFileGetRequest(writer, path);
                } else if (method.equals("POST")) {
                    handleFilePostRequest(reader, writer, path, contentLength);
                } else {
                    sendErrorResponse(writer, 405, "Method Not Allowed");
                }
            } else {
                sendErrorResponse(writer, 404, "Not Found");
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Socket close error: " + e.getMessage());
            }
        }
    }

    private static void handleEchoRequest(PrintWriter writer, String path) {
        String echoStr = path.substring(6);
        sendResponse(writer, 200, "OK", echoStr);
    }

    private static void handleUserAgentRequest(PrintWriter writer, String userAgent) {
        if (userAgent == null) {
            userAgent = "";
        }
        sendResponse(writer, 200, "OK", userAgent);
    }

    private static void handleFileGetRequest(PrintWriter writer, String path) {
        String filename = path.substring(7); // Remove "/files/" prefix

        try {
            Path filePath = Paths.get(directory, filename);

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendErrorResponse(writer, 404, "Not Found");
                return;
            }

            byte[] fileContent = Files.readAllBytes(filePath);
            String content = new String(fileContent);
            sendFileResponse(writer, 200, "OK", content);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            sendErrorResponse(writer, 500, "Internal Server Error");
        }
    }

    private static void handleFilePostRequest(BufferedReader reader, PrintWriter writer, String path, int contentLength) throws IOException {
        String filename = path.substring(7); // Remove "/files/" prefix
        Path filePath = Paths.get(directory, filename);

        // Read the request body
        char[] buffer = new char[contentLength];
        int bytesRead = reader.read(buffer, 0, contentLength);
        String fileContent = new String(buffer, 0, bytesRead);

        try {
            // Write the content to file
            Files.write(filePath, fileContent.getBytes());
            sendResponse(writer, 201, "Created", null);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
            sendErrorResponse(writer, 500, "Internal Server Error");
        }
    }

    private static void sendResponse(PrintWriter writer, int statusCode, String statusText, String body) {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");

        if (body != null) {
            response.append("Content-Type: text/plain\r\n");
            response.append("Content-Length: ").append(body.length()).append("\r\n");
        } else {
            response.append("Content-Length: 0\r\n");
        }
        response.append("Connection: close\r\n\r\n");

        if (body != null) {
            response.append(body);
        }

        writer.print(response.toString());
    }

    private static void sendFileResponse(PrintWriter writer, int statusCode, String statusText, String body) {
        StringBuilder response = new StringBuilder();
        response.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");

        if (body != null) {
            response.append("Content-Type: application/octet-stream\r\n");
            response.append("Content-Length: ").append(body.length()).append("\r\n");
        } else {
            response.append("Content-Length: 0\r\n");
        }
        response.append("Connection: close\r\n\r\n");

        if (body != null) {
            response.append(body);
        }

        writer.print(response.toString());
    }

    private static void sendErrorResponse(PrintWriter writer, int statusCode, String statusText) {
        sendResponse(writer, statusCode, statusText, null);
    }
}