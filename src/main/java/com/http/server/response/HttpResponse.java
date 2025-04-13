package com.http.server.response;

import java.util.HashMap;
import java.util.Map;

public class HttpResponse {
    private final int statusCode;
    private final String statusText;
    private final Map<String, String> headers;
    private final byte[] body;
    private final boolean useGzip;
    private final boolean keepAlive;

    private HttpResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.statusText = builder.statusText;
        this.headers = builder.headers;
        this.body = builder.body;
        this.useGzip = builder.useGzip;
        this.keepAlive = builder.keepAlive;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }

    public boolean isUseGzip() {
        return useGzip;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public static class Builder {
        private int statusCode;
        private String statusText;
        private final Map<String, String> headers = new HashMap<>();
        private byte[] body;
        private boolean useGzip;
        private boolean keepAlive = true;

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder statusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        public Builder header(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder body(byte[] body) {
            this.body = body;
            return this;
        }

        public Builder useGzip(boolean useGzip) {
            this.useGzip = useGzip;
            return this;
        }

        public Builder keepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public HttpResponse build() {
            return new HttpResponse(this);
        }
    }
} 