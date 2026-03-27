package com.javaserver.http;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Request {
    private String method;
    private String path;
    private String version;
    private Map<String, String> headers;
    private String body;
    private byte[] rawBody; // ← body en bytes bruts

    private Request() { this.headers = new HashMap<>(); }

    public static Request parse(byte[] raw) {
        Request req = new Request();

        // Chercher \r\n\r\n en bytes
        byte[] separator = "\r\n\r\n".getBytes();
        int headerEnd = findBytes(raw, separator);
        if (headerEnd == -1) {
            throw new RuntimeException("Invalid request: no header separator");
        }

        // Headers en String (ASCII)
        String headerStr = new String(raw, 0, headerEnd);
        String[] lines = headerStr.split("\r\n");

        // Ligne de démarrage
        String[] start = lines[0].split(" ", 3);
        if (start.length < 3) {
            throw new RuntimeException("Invalid request line: " + lines[0]);
        }
        req.method  = start[0];
        req.path    = start[1];
        req.version = start[2];

        // Headers
        for (int i = 1; i < lines.length; i++) {
            String[] h = lines[i].split(":", 2);
            if (h.length == 2)
                req.headers.put(h[0].trim(), h[1].trim());
        }

        // Body — garder en bytes bruts
        int bodyStart = headerEnd + 4;
        req.rawBody = Arrays.copyOfRange(raw, bodyStart, raw.length);

        // Body en String pour compatibilité (CGI, forms)
        String transferEncoding = req.headers.getOrDefault("Transfer-Encoding", "");
        if (transferEncoding.equalsIgnoreCase("chunked")) {
            req.body = decodeChunked(new String(req.rawBody));
        } else {
            req.body = new String(req.rawBody);
        }

        return req;
    }

    // Garder l'ancienne méthode pour compatibilité
    public static Request parse(String raw) {
        return parse(raw.getBytes());
    }

    private static int findBytes(byte[] haystack, byte[] needle) {
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) { found = false; break; }
            }
            if (found) return i;
        }
        return -1;
    }

    private static String decodeChunked(String raw) {
        StringBuilder result = new StringBuilder();
        int pos = 0;
        while (pos < raw.length()) {
            int lineEnd = raw.indexOf("\r\n", pos);
            if (lineEnd == -1) break;
            String sizeLine = raw.substring(pos, lineEnd).trim();
            if (sizeLine.isEmpty()) break;
            int chunkSize;
            try { chunkSize = Integer.parseInt(sizeLine, 16); }
            catch (NumberFormatException e) { break; }
            if (chunkSize == 0) break;
            pos = lineEnd + 2;
            if (pos + chunkSize > raw.length()) break;
            result.append(raw, pos, pos + chunkSize);
            pos += chunkSize + 2;
        }
        return result.toString();
    }

    public String getMethod()   { return method; }
    public String getPath()     { return path; }
    public String getVersion()  { return version; }
    public Map<String, String> getHeaders() { return headers; }
    public String getBody()     { return body; }
    public byte[] getRawBody()  { return rawBody; } // ← nouveau getter
}