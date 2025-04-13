package com.http.server.handler;

import com.http.server.request.HttpRequest;
import com.http.server.request.RequestParser;
import com.http.server.response.HttpResponse;
import com.http.server.response.ResponseBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ConnectionHandler {
    private final Socket clientSocket;
    private final String directory;

    public ConnectionHandler(Socket clientSocket, String directory) {
        this.clientSocket = clientSocket;
        this.directory = directory;
    }

    public void handle() {
        try (var reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            boolean keepAlive = true;

            while (keepAlive) {
                var requestLine = reader.readLine();
                if (requestLine == null) {
                    break;
                }

                if (requestLine.trim().isEmpty()) {
                    continue;
                }

                keepAlive = processRequest(reader, requestLine);
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

    private boolean processRequest(BufferedReader reader, String requestLine) throws IOException {
        var request = RequestParser.parseRequest(reader, requestLine);
        var response = new RequestHandler(directory).handleRequest(request);
        ResponseBuilder.sendResponse(clientSocket, response);
        return request.isKeepAlive();
    }
} 