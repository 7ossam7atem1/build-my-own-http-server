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

        System.out.println("Server starting on port " + port + " with directory: " + directory);

        ExecutorService threadPool = Executors.newFixedThreadPool(10);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server ready for connections...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(() -> handleConnection(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }

    private static void handleConnection(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            boolean keepAlive = true;

            while (keepAlive) {
                // Try to read the request line
                String requestLine = reader.readLine();

                // If we get null, the client has closed the connection
                if (requestLine == null) {
                    break;
                }

                // Skip empty lines
                if (requestLine.trim().isEmpty()) {
                    continue;
                }

                // Process the request
                keepAlive = handleRequest(clientSocket, reader, requestLine);
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

    private static boolean handleRequest(Socket clientSocket, BufferedReader reader, String requestLine) throws IOException {
        if (requestLine == null || requestLine.isEmpty()) {
            sendErrorResponse(clientSocket, 400, "Bad Request", true);
            return false;
        }

        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 2) {
            sendErrorResponse(clientSocket, 400, "Bad Request", true);
            return false;
        }

        String method = requestParts[0];
        String path = requestParts[1];
        String userAgent = null;
        int contentLength = 0;
        boolean acceptGzip = false;
        boolean keepAlive = true;

        // Read headers
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            if (headerLine.startsWith("User-Agent: ")) {
                userAgent = headerLine.substring(12);
            } else if (headerLine.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(headerLine.substring(16).trim());
            } else if (headerLine.startsWith("Accept-Encoding: ")) {
                acceptGzip = headerLine.substring(17).contains("gzip");
            } else if (headerLine.equalsIgnoreCase("Connection: close")) {
                keepAlive = false;
            }
        }

        // Handle different endpoints
        if (path.equals("/")) {
            sendResponse(clientSocket, 200, "OK", null, acceptGzip, keepAlive);
        } else if (path.startsWith("/echo/")) {
            handleEchoRequest(clientSocket, path, acceptGzip, keepAlive);
        } else if (path.equals("/user-agent")) {
            handleUserAgentRequest(clientSocket, userAgent, acceptGzip, keepAlive);
        } else if (path.startsWith("/files/")) {
            if (method.equals("GET")) {
                handleFileGetRequest(clientSocket, path, acceptGzip, keepAlive);
            } else if (method.equals("POST")) {
                handleFilePostRequest(reader, clientSocket, path, contentLength, keepAlive);
            } else {
                sendErrorResponse(clientSocket, 405, "Method Not Allowed", keepAlive);
            }
        } else {
            sendErrorResponse(clientSocket, 404, "Not Found", keepAlive);
        }

        return keepAlive;
    }

    private static void handleEchoRequest(Socket clientSocket, String path, boolean acceptGzip, boolean keepAlive) throws IOException {
        String echoStr = path.substring(6);
        sendResponse(clientSocket, 200, "OK", echoStr, acceptGzip, keepAlive);
    }

    private static void handleUserAgentRequest(Socket clientSocket, String userAgent, boolean acceptGzip, boolean keepAlive) throws IOException {
        if (userAgent == null) {
            userAgent = "";
        }
        sendResponse(clientSocket, 200, "OK", userAgent, acceptGzip, keepAlive);
    }

    private static void handleFileGetRequest(Socket clientSocket, String path, boolean acceptGzip, boolean keepAlive) throws IOException {
        String filename = path.substring(7);
        try {
            Path filePath = Paths.get(directory, filename);
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                sendErrorResponse(clientSocket, 404, "Not Found", keepAlive);
                return;
            }
            byte[] fileContent = Files.readAllBytes(filePath);
            sendFileResponse(clientSocket, 200, "OK", fileContent, acceptGzip, keepAlive);
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            sendErrorResponse(clientSocket, 500, "Internal Server Error", keepAlive);
        }
    }

    private static void handleFilePostRequest(BufferedReader reader, Socket clientSocket, String path, int contentLength, boolean keepAlive) throws IOException {
        String filename = path.substring(7);
        Path filePath = Paths.get(directory, filename);

        char[] buffer = new char[contentLength];
        int bytesRead = reader.read(buffer, 0, contentLength);
        String fileContent = new String(buffer, 0, bytesRead);

        try {
            Files.write(filePath, fileContent.getBytes());
            sendResponse(clientSocket, 201, "Created", null, false, keepAlive);
        } catch (IOException e) {
            System.err.println("Error writing file: " + e.getMessage());
            sendErrorResponse(clientSocket, 500, "Internal Server Error", keepAlive);
        }
    }

    private static void sendResponse(Socket clientSocket, int statusCode, String statusText, String body, boolean useGzip, boolean keepAlive) throws IOException {
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

        if (!keepAlive) {
            headerBuilder.append("Connection: close\r\n");
        }
        headerBuilder.append("\r\n");

        output.write(headerBuilder.toString().getBytes());
        if (bodyBytes != null) {
            output.write(bodyBytes);
        }
        output.flush();
    }

    private static void sendFileResponse(Socket clientSocket, int statusCode, String statusText, byte[] body, boolean useGzip, boolean keepAlive) throws IOException {
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

        if (!keepAlive) {
            headerBuilder.append("Connection: close\r\n");
        }
        headerBuilder.append("\r\n");

        output.write(headerBuilder.toString().getBytes());
        if (bodyBytes != null) {
            output.write(bodyBytes);
        }
        output.flush();
    }

    private static void sendErrorResponse(Socket clientSocket, int statusCode, String statusText, boolean keepAlive) throws IOException {
        sendResponse(clientSocket, statusCode, statusText, null, false, keepAlive);
    }
}