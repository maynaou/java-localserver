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

    public static HttpRequest parse(String buffer) {
        HttpRequest req = new HttpRequest();

        String[] parts = buffer.split("\r\n\r\n", 2);
        String[] lines = parts[0].split("\r\n");

        String[] requestLine = lines[0].split(" ");
        req.method = requestLine[0]; // GET, POST, DELETE
        req.path = requestLine[1]; // /index.html
        req.version = requestLine[2]; // HTTP/1.1
        return req;
    }

}
