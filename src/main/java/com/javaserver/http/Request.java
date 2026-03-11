package com.javaserver.http;

import java.util.HashMap;
import java.util.Map;

public class Request {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private String body;

    private Request() { this.headers = new HashMap<>(); }

    public static Request parse(String raw) {
        Request req = new Request();
        String[] parts = raw.split("\r\n\r\n", 2);
        String[] headerLines = parts[0].split("\r\n");

        // Ligne de démarrage : GET /index.html HTTP/1.1
        String[] start = headerLines[0].split(" ", 3);
        req.method  = start[0];
        req.path    = start[1];
        req.version = start[2];

        // Headers
        for (int i = 1; i < headerLines.length; i++) {
            String[] h = headerLines[i].split(":", 2);
            if (h.length == 2)
                req.headers.put(h[0].trim(), h[1].trim());
        }

        // Body (POST)
        req.body = (parts.length > 1) ? parts[1] : "";
        return req;
    }

    public String getMethod()  { return method; }
    public String getPath()    { return path; }
    public String getVersion() { return version; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody()    { return body; }
}