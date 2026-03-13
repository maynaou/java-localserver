package com.javaserver.http;

import java.util.LinkedHashMap;
import java.util.Map;

public class HttpResponse {

    private int statusCode;
    private String statusMessage;
    private Map<String, String> headers = new LinkedHashMap<>();
    private byte[] body = new byte[0];

    // ─── Static factory methods ───────────────────────────────────────────────

    public static HttpResponse ok()                  { return new HttpResponse(200, "OK"); }
    public static HttpResponse created()             { return new HttpResponse(201, "Created"); }
    public static HttpResponse noContent()           { return new HttpResponse(204, "No Content"); }
    public static HttpResponse notFound()            { return new HttpResponse(404, "Not Found"); }
    public static HttpResponse methodNotAllowed()    { return new HttpResponse(405, "Method Not Allowed"); }
    public static HttpResponse badRequest()          { return new HttpResponse(400, "Bad Request"); }
    public static HttpResponse forbidden()           { return new HttpResponse(403, "Forbidden"); }
    public static HttpResponse internalError()       { return new HttpResponse(500, "Internal Server Error"); }
    public static HttpResponse tooLarge()            { return new HttpResponse(413, "Request Entity Too Large"); }

    public static HttpResponse redirect(int code, String location) {
        HttpResponse res = new HttpResponse(code, code == 301 ? "Moved Permanently" : "Found");
        res.setHeader("Location", location);
        return res;
    }

    // ─── Constructor ──────────────────────────────────────────────────────────

    public HttpResponse(int statusCode, String statusMessage) {
        this.statusCode    = statusCode;
        this.statusMessage = statusMessage;
    }

    // ─── Builder-style setters (return this for chaining) ─────────────────────

    public HttpResponse setHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public HttpResponse setBody(byte[] body, String contentType) {
        this.body = body;
        setHeader("Content-Type", contentType);
        setHeader("Content-Length", String.valueOf(body.length));
        return this;
    }

    public HttpResponse setBody(String body, String contentType) {
        return setBody(body.getBytes(), contentType);
    }

    // ─── Serialize to bytes ───────────────────────────────────────────────────

    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();

        // Status line
        sb.append("HTTP/1.1 ").append(statusCode).append(" ").append(statusMessage).append("\r\n");

        // Headers
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }

        // Blank line
        sb.append("\r\n");

        // Headers as bytes + body
        byte[] headerBytes = sb.toString().getBytes();
        byte[] result = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, result, 0, headerBytes.length);
        System.arraycopy(body, 0, result, headerBytes.length, body.length);

        return result;
    }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public int getStatusCode()            { return statusCode; }
    public String getStatusMessage()      { return statusMessage; }
    public Map<String, String> getHeaders() { return headers; }
    public byte[] getBody()               { return body; }
}