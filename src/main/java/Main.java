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
                sendResponse(writer, 400, "Bad Request");
                return;
            }


            String[] requestParts = requestLine.split(" ");
            if (requestParts.length < 2) {
                sendResponse(writer, 400, "Bad Request");
                return;
            }

            String method = requestParts[0];
            String path = requestParts[1];


            if (path.equals("/")) {
                sendResponse(writer, 200, "OK");
            } else {
                sendResponse(writer, 404, "Not Found");
            }

        } catch (IOException e) {
            System.err.println("Client handling error: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Socket close error: " + e.getMessage());
            }
        }
    }

    private static void sendResponse(PrintWriter writer, int statusCode, String statusText) {
        String response = String.format(
                "HTTP/1.1 %d %s\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n",
                statusCode, statusText
        );
        writer.write(response);
        writer.flush();
    }
}