import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    private static final int DEFAULT_PORT = 4221;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        System.out.println("Server starting on port " + port);

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

            String path = requestParts[1];
            String userAgent = null;


            String headerLine;
            while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.startsWith("User-Agent: ")) {
                    userAgent = headerLine.substring(12); // Remove "User-Agent: " prefix
                }
            }


            if (path.equals("/")) {
                sendResponse(writer, 200, "OK", null);
            } else if (path.startsWith("/echo/")) {
                handleEchoRequest(writer, path);
            } else if (path.equals("/user-agent")) {
                handleUserAgentRequest(writer, userAgent);
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

    private static void sendErrorResponse(PrintWriter writer, int statusCode, String statusText) {
        sendResponse(writer, statusCode, statusText, null);
    }
}