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

            // Body — vérifier si chunked
    String rawBody = (parts.length > 1) ? parts[1] : "";
    String transferEncoding = req.headers.getOrDefault("Transfer-Encoding", "");

    if (transferEncoding.equalsIgnoreCase("chunked")) {
        // ✅ Décoder le body chunked
        req.body = decodeChunked(rawBody);
    } else {
        req.body = rawBody;
    }

      return req;
    }

    private static String decodeChunked(String raw) {
    StringBuilder result = new StringBuilder();
    int pos = 0;

    while (pos < raw.length()) {
        // 1. Lire la taille du chunk en hex
        int lineEnd = raw.indexOf("\r\n", pos);
        if (lineEnd == -1) break;

        String sizeLine = raw.substring(pos, lineEnd).trim();
        if (sizeLine.isEmpty()) break;

        int chunkSize;
        try {
            chunkSize = Integer.parseInt(sizeLine, 16); // hex → int
        } catch (NumberFormatException e) {
            break;
        }

        // 2. Chunk de taille 0 → fin
        if (chunkSize == 0) break;

        // 3. Lire les données du chunk
        pos = lineEnd + 2;
        if (pos + chunkSize > raw.length()) break;

        result.append(raw, pos, pos + chunkSize);
        pos += chunkSize + 2; // +2 pour \r\n après les données
    }

    return result.toString();
}

    public String getMethod()  { return method; }
    public String getPath()    { return path; }
    public String getVersion() { return version; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody()    { return body; }
}