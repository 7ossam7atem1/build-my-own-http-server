package com.http.server.handler;

import com.http.server.request.HttpRequest;
import com.http.server.response.HttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RequestHandler {
    private final String directory;

    public RequestHandler(String directory) {
        this.directory = directory;
    }

    public HttpResponse handleRequest(HttpRequest request) {
        String path = request.getPath();
        String method = request.getMethod();

        try {
            if (path.equals("/")) {
                return handleRootRequest(request);
            } else if (path.startsWith("/echo/")) {
                return handleEchoRequest(request);
            } else if (path.equals("/user-agent")) {
                return handleUserAgentRequest(request);
            } else if (path.startsWith("/files/")) {
                if (method.equals("GET")) {
                    return handleFileGetRequest(request);
                } else if (method.equals("POST")) {
                    return handleFilePostRequest(request);
                } else {
                    return createErrorResponse(405, "Method Not Allowed", request.isKeepAlive());
                }
            } else {
                return createErrorResponse(404, "Not Found", request.isKeepAlive());
            }
        } catch (IOException e) {
            return createErrorResponse(500, "Internal Server Error", request.isKeepAlive());
        }
    }

    private HttpResponse handleRootRequest(HttpRequest request) {
        return new HttpResponse.Builder()
                .statusCode(200)
                .statusText("OK")
                .keepAlive(request.isKeepAlive())
                .build();
    }

    private HttpResponse handleEchoRequest(HttpRequest request) {
        String echoStr = request.getPath().substring(6);
        return new HttpResponse.Builder()
                .statusCode(200)
                .statusText("OK")
                .body(echoStr.getBytes())
                .useGzip(request.acceptsGzip())
                .keepAlive(request.isKeepAlive())
                .build();
    }

    private HttpResponse handleUserAgentRequest(HttpRequest request) {
        String userAgent = request.getUserAgent();
        if (userAgent == null) {
            userAgent = "";
        }
        return new HttpResponse.Builder()
                .statusCode(200)
                .statusText("OK")
                .body(userAgent.getBytes())
                .useGzip(request.acceptsGzip())
                .keepAlive(request.isKeepAlive())
                .build();
    }

    private HttpResponse handleFileGetRequest(HttpRequest request) throws IOException {
        String filename = request.getPath().substring(7);
        Path filePath = Paths.get(directory, filename);
        
        if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
            return createErrorResponse(404, "Not Found", request.isKeepAlive());
        }

        byte[] fileContent = Files.readAllBytes(filePath);
        return new HttpResponse.Builder()
                .statusCode(200)
                .statusText("OK")
                .body(fileContent)
                .header("Content-Type", "application/octet-stream")
                .useGzip(request.acceptsGzip())
                .keepAlive(request.isKeepAlive())
                .build();
    }

    private HttpResponse handleFilePostRequest(HttpRequest request) throws IOException {
        String filename = request.getPath().substring(7);
        Path filePath = Paths.get(directory, filename);
        
        Files.write(filePath, request.getBody().getBytes());
        
        return new HttpResponse.Builder()
                .statusCode(201)
                .statusText("Created")
                .keepAlive(request.isKeepAlive())
                .build();
    }

    private HttpResponse createErrorResponse(int statusCode, String statusText, boolean keepAlive) {
        return new HttpResponse.Builder()
                .statusCode(statusCode)
                .statusText(statusText)
                .keepAlive(keepAlive)
                .build();
    }
} 