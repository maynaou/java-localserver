package com.javaserver.http;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers = new HashMap<>();
    private byte[] body;

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public byte[] getBody() {
        return body;
    }


        public void setMethod(String method) {
        this.method = method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public static HttpRequest parse(String buffer) {
        HttpRequest req = new HttpRequest();

        String[] parts = buffer.split("\r\n\r\n", 2);
        String[] lines = parts[0].split("\r\n");

        String[] requestLine = lines[0].split(" ");
        req.method = requestLine[0]; // GET, POST, DELETE
        req.path = requestLine[1]; // /index.html
        req.version = requestLine[2]; // HTTP/1.1

        for (int i = 1; i < lines.length; i++) {
            String[] header = lines[i].split(": ", 2);
            if (header.length == 2) {
                req.headers.put(header[0], header[1]);
            }
        }

        if (parts.length > 1) {
            req.body = parts[1].getBytes();
        }

        return req;
    }

}
