package com.javaserver.http;

import java.util.HashMap;
import java.util.Map;

public class Response {
    private int status;
    private String statusText;
    private String contentType;
    private byte[] body;
    private Map<String, String> extraHeaders = new HashMap<>();
    private boolean keepAlive = false;


    public Response(int status, String statusText, String contentType, byte[] body) {
        this.status      = status;
        this.statusText  = statusText;
        this.contentType = contentType;
        this.body        = body;
    }

    public void addHeader(String key, String value) {
        extraHeaders.put(key, value);
    }

    public void setKeepAlive(boolean keep) {
        this.keepAlive = keep;
    }


    public byte[] toBytes() {
        StringBuilder h = new StringBuilder();
        h.append("HTTP/1.1 ").append(status).append(" ").append(statusText).append("\r\n");
        h.append("Content-Type: ").append(contentType).append("\r\n");
        h.append("Content-Length: ").append(body.length).append("\r\n");
        h.append("Connection: ").append(keepAlive ? "keep-alive" : "close").append("\r\n");

        // Headers extra (Set-Cookie etc.)
        for (Map.Entry<String, String> entry : extraHeaders.entrySet()) {
            h.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        h.append("\r\n");
        byte[] headerBytes = h.toString().getBytes();
        byte[] full = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, full, 0, headerBytes.length);
        System.arraycopy(body, 0, full, headerBytes.length, body.length);
        return full;
    }
}