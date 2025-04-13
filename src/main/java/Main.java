import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(clientSocket, 400, "Bad Request");
                return;
            }

            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendErrorResponse(clientSocket, 400, "Bad Request");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];
            String userAgent = null;
            int contentLength = 0;
            boolean acceptGzip = false;

            // Read headers
            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.startsWith("User-Agent: ")) {
                    userAgent = headerLine.substring(12);
                } else if (headerLine.startsWith("Content-Length: ")) {
                    contentLength = Integer.parseInt(headerLine.substring(16).trim());
                } else if (headerLine.startsWith("Accept-Encoding: ")) {
                    acceptGzip = headerLine.substring(17).contains("gzip");
                }
            }

            // Handle different endpoints
            if (path.equals("/")) {
                sendResponse(clientSocket, 200, "OK", null, acceptGzip);
            } else if (path.startsWith("/echo/")) {
                handleEchoRequest(clientSocket, path, acceptGzip);
            } else if (path.equals("/user-agent")) {
                handleUserAgentRequest(clientSocket, userAgent, acceptGzip);
            } else if (path.startsWith("/files/")) {
                if (method.equals("GET")) {
                    handleFileGetRequest(clientSocket, path, acceptGzip);
                } else if (method.equals("POST")) {
                    handleFilePostRequest(reader, clientSocket, path, contentLength);
                } else {
                    sendErrorResponse(clientSocket, 405, "Method Not Allowed");
                }
            } else {
                sendErrorResponse(clientSocket, 404, "Not Found");
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

    private static void handleEchoRequest(Socket clientSocket, String path, boolean acceptGzip) throws IOException {
        String echoStr = path.substring(6);
        sendResponse(clientSocket, 200, "OK", echoStr, acceptGzip);
    }

    private static void handleUserAgentRequest(Socket clientSocket, String userAgent, boolean acceptGzip) throws IOException {
        if (userAgent == null) {
            userAgent = "";
        }
        sendResponse(clientSocket, 200, "OK", userAgent, acceptGzip);
    }

    private static void handleFileGetRequest(Socket clientSocket, String path, boolean acceptGzip) throws IOException {
        String filename = path.substring(7);
        try {
            Path filePath = Paths.get(directory, filename);
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendErrorResponse(clientSocket, 404, "Not Found");
                return;
            }
            byte[] fileContent = Files.readAllBytes(filePath);
            sendFileResponse(clientSocket, 200, "OK", fileContent, acceptGzip);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            sendErrorResponse(clientSocket, 500, "Internal Server Error");
        }
    }

    private static void handleFilePostRequest(BufferedReader reader, Socket clientSocket, String path, int contentLength) throws IOException {
        String filename = path.substring(7);
        Path filePath = Paths.get(directory, filename);

        char[] buffer = new char[contentLength];
        int bytesRead = reader.read(buffer, 0, contentLength);
        String fileContent = new String(buffer, 0, bytesRead);

        try {
            Files.write(filePath, fileContent.getBytes());
            sendResponse(clientSocket, 201, "Created", null, false);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
            sendErrorResponse(clientSocket, 500, "Internal Server Error");
        }
    }

    private static void sendResponse(Socket clientSocket, int statusCode, String statusText, String body, boolean useGzip) throws IOException {
        OutputStream output = clientSocket.getOutputStream();
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");

        byte[] bodyBytes = null;
        if (body != null) {
            if (useGzip) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
                    gzipStream.write(body.getBytes());
                }
                bodyBytes = byteStream.toByteArray();
                headerBuilder.append("Content-Encoding: gzip\r\n");
            } else {
                bodyBytes = body.getBytes();
            }
            headerBuilder.append("Content-Type: text/plain\r\n");
            headerBuilder.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        } else {
            headerBuilder.append("Content-Length: 0\r\n");
        }
        headerBuilder.append("Connection: close\r\n\r\n");

        output.write(headerBuilder.toString().getBytes());
        if (bodyBytes != null) {
            output.write(bodyBytes);
        }
        output.flush();
    }

    private static void sendFileResponse(Socket clientSocket, int statusCode, String statusText, byte[] body, boolean useGzip) throws IOException {
        OutputStream output = clientSocket.getOutputStream();
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusText).append("\r\n");

        byte[] bodyBytes = null;
        if (body != null) {
            if (useGzip) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
                    gzipStream.write(body);
                }
                bodyBytes = byteStream.toByteArray();
                headerBuilder.append("Content-Encoding: gzip\r\n");
            } else {
                bodyBytes = body;
            }
            headerBuilder.append("Content-Type: application/octet-stream\r\n");
            headerBuilder.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        } else {
            headerBuilder.append("Content-Length: 0\r\n");
        }
        headerBuilder.append("Connection: close\r\n\r\n");

        output.write(headerBuilder.toString().getBytes());
        if (bodyBytes != null) {
            output.write(bodyBytes);
        }
        output.flush();
    }

    private static void sendErrorResponse(Socket clientSocket, int statusCode, String statusText) throws IOException {
        sendResponse(clientSocket, statusCode, statusText, null, false);
    }
}