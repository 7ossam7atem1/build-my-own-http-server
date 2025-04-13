package com.http.server.request;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestParser {
    public static HttpRequest parseRequest(BufferedReader reader, String requestLine) throws IOException {
        String[] requestParts = requestLine.split(" ");
        if (requestParts.length < 2) {
            throw new IllegalArgumentException("Invalid request line: " + requestLine);
        }

        String method = requestParts[0];
        String path = requestParts[1];
        Map<String, String> headers = new HashMap<>();
        boolean keepAlive = true;

        // Read headers
        String headerLine;
        while ((headerLine = reader.readLine()) != null && !headerLine.isEmpty()) {
            int colonIndex = headerLine.indexOf(':');
            if (colonIndex > 0) {
                String headerName = headerLine.substring(0, colonIndex).trim();
                String headerValue = headerLine.substring(colonIndex + 1).trim();
                headers.put(headerName, headerValue);
                
                if (headerName.equalsIgnoreCase("Connection") && headerValue.equalsIgnoreCase("close")) {
                    keepAlive = false;
                }
            }
        }

        // Read body if present
        String body = null;
        String contentLength = headers.get("Content-Length");
        if (contentLength != null) {
            int length = Integer.parseInt(contentLength);
            if (length > 0) {
                char[] buffer = new char[length];
                int bytesRead = reader.read(buffer, 0, length);
                body = new String(buffer, 0, bytesRead);
            }
        }

        return new HttpRequest(method, path, headers, body, keepAlive);
    }
} 