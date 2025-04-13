package com.http.server.request;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;
    private final boolean keepAlive;

    public HttpRequest(String method, String path, Map<String, String> headers, String body, boolean keepAlive) {
        this.method = method;
        this.path = path;
        this.headers = headers;
        this.body = body;
        this.keepAlive = keepAlive;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public String getBody() {
        return body;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public boolean acceptsGzip() {
        String acceptEncoding = getHeader("Accept-Encoding");
        return acceptEncoding != null && acceptEncoding.contains("gzip");
    }

    public String getUserAgent() {
        return getHeader("User-Agent");
    }

    public int getContentLength() {
        String contentLength = getHeader("Content-Length");
        return contentLength != null ? Integer.parseInt(contentLength) : 0;
    }
} 