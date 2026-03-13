package com.javaserver.http;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {

    private String method;
    private String path;
    private String queryString = "";
    private String version;
    private boolean isChunked = false;
    private boolean isMultipart = false;
    private Map<String, String> headers = new HashMap<>();
    private byte[] body = new byte[0];

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getQueryString() {
        return queryString;
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

    public boolean isChunked() {
        return isChunked;
    }

    public boolean isMultipart() {
        return isMultipart;
    }

    public String getHeader(String name) {
        return headers.getOrDefault(name.toLowerCase(), null);
    }

    // ─── Setters ──────────────────────────────────────────────────────────────

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

    public static HttpRequest parse(String raw) {
        HttpRequest req = new HttpRequest();

        // Split headers from body on blank line
        int splitIndex = raw.indexOf("\r\n\r\n");
        if (splitIndex == -1) {
            throw new HttpParseException(400, "Malformed request: missing blank line");
        }

        String headerSection = raw.substring(0, splitIndex);
        String rawBody = raw.substring(splitIndex + 4);

        String[] lines = headerSection.split("\r\n");
        if (lines.length == 0) {
            throw new HttpParseException(400, "Empty request");
        }

        // ── 1. Request line ───────────────────────────────────────────────────
        String[] requestLine = lines[0].split(" ");
        if (requestLine.length != 3) {
            throw new HttpParseException(400, "Invalid request line: " + lines[0]);
        }

        req.method = requestLine[0].toUpperCase();
        req.version = requestLine[2];

        // Split path and query string: /search?q=java → path=/search, query=q=java
        String fullPath = requestLine[1];
        int qMark = fullPath.indexOf('?');
        if (qMark >= 0) {
            req.path = fullPath.substring(0, qMark);
            req.queryString = fullPath.substring(qMark + 1);
        } else {
            req.path = fullPath;
        }

        // ── 2. Headers ────────────────────────────────────────────────────────

        for (int i = 1; i < lines.length; i++) {
            int colon = lines[i].indexOf(':');
            if (colon < 0)
                continue;
            String key = lines[i].substring(0, colon).trim().toLowerCase();
            String value = lines[i].substring(colon + 1).trim();
            req.headers.put(key, value);
        }

        String host = req.headers.get("host");
        if (host == null || host.trim().isEmpty()) {
            throw new HttpParseException(400, "Missing Host header (required for HTTP/1.1)");
        }

        // ── 3. Body ────────────────────────────────────────────────────────
        // ── 3. Body ───────────────────────────────────────────────────────────
        String transferEncoding = req.headers.getOrDefault("transfer-encoding", "");
        String contentLengthStr = req.headers.get("content-length");

        if (transferEncoding.equalsIgnoreCase("chunked")) {
            req.isChunked = true;
            req.body = decodeChunked(rawBody).getBytes();

        } else if (contentLengthStr != null) {
            long contentLength;
            try {
                contentLength = Long.parseLong(contentLengthStr.trim());
            } catch (NumberFormatException e) {
                throw new HttpParseException(400, "Invalid Content-Length value");
            }

            byte[] rawBytes = rawBody.getBytes();
            int readLen = (int) Math.min(contentLength, rawBytes.length);
            req.body = new byte[readLen];
            System.arraycopy(rawBytes, 0, req.body, 0, readLen);
        }

        String contentType = req.headers.getOrDefault("content-type", "");
        if (contentType.startsWith("multipart/form-data")) {
            req.isMultipart = true;
        }

        return req;

    }

    private static String decodeChunked(String raw) {
        StringBuilder result = new StringBuilder();
        int i = 0;

        while (i < raw.length()) {
            int lineEnd = raw.indexOf("\r\n", i);
            if (lineEnd == -1)
                break;

            String hexSize = raw.substring(i, lineEnd).trim();
            int chunkSize;
            try {
                chunkSize = Integer.parseInt(hexSize, 16);
            } catch (NumberFormatException e) {
                throw new HttpParseException(400, "Invalid chunk size: " + hexSize);
            }

            if (chunkSize == 0)
                break;

            int dataStart = lineEnd + 2;
            int dataEnd = dataStart + chunkSize;

            if (dataEnd > raw.length()) {
                throw new HttpParseException(400, "Chunk data shorter than declared size");
            }

            result.append(raw, dataStart, dataEnd);
            i = dataEnd + 2;
        }

        return result.toString();
    }

    public static class HttpParseException extends RuntimeException {
        public final int statusCode;

        public HttpParseException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }
    }

}
