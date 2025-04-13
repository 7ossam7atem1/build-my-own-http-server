package com.http.server.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.zip.GZIPOutputStream;

public class ResponseBuilder {
    public static void sendResponse(Socket clientSocket, HttpResponse response) throws IOException {
        OutputStream output = clientSocket.getOutputStream();
        StringBuilder headerBuilder = new StringBuilder();
        headerBuilder.append("HTTP/1.1 ")
                    .append(response.getStatusCode())
                    .append(" ")
                    .append(response.getStatusText())
                    .append("\r\n");

        byte[] bodyBytes = response.getBody();
        if (bodyBytes != null) {
            if (response.isUseGzip()) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
                    gzipStream.write(bodyBytes);
                }
                bodyBytes = byteStream.toByteArray();
                headerBuilder.append("Content-Encoding: gzip\r\n");
            }
            headerBuilder.append("Content-Type: ")
                        .append(response.getHeaders().getOrDefault("Content-Type", "text/plain"))
                        .append("\r\n");
            headerBuilder.append("Content-Length: ")
                        .append(bodyBytes.length)
                        .append("\r\n");
        } else {
            headerBuilder.append("Content-Length: 0\r\n");
        }

        if (!response.isKeepAlive()) {
            headerBuilder.append("Connection: close\r\n");
        }

        // Add custom headers
        response.getHeaders().forEach((name, value) -> {
            if (!name.equals("Content-Type")) {
                headerBuilder.append(name).append(": ").append(value).append("\r\n");
            }
        });

        headerBuilder.append("\r\n");

        output.write(headerBuilder.toString().getBytes());
        if (bodyBytes != null) {
            output.write(bodyBytes);
        }
        output.flush();
    }
} 