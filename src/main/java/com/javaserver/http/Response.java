package com.javaserver.http;

public class Response {
    private int status;
    private String statusText;
    private String contentType;
    private byte[] body;

    public Response(int status, String statusText, String contentType, byte[] body) {
        this.status      = status;
        this.statusText  = statusText;
        this.contentType = contentType;
        this.body        = body;
    }

    public byte[] toBytes() {
        String headers = "HTTP/1.1 " + status + " " + statusText + "\r\n"
            + "Content-Type: "   + contentType + "\r\n"
            + "Content-Length: " + body.length + "\r\n"
            + "Connection: close\r\n"
            + "\r\n";
        byte[] headerBytes = headers.getBytes();
        byte[] full = new byte[headerBytes.length + body.length];
        System.arraycopy(headerBytes, 0, full, 0, headerBytes.length);
        System.arraycopy(body, 0, full, headerBytes.length, body.length);
        return full;
    }
}