package com.javaserver.server;

import java.nio.ByteBuffer;
import com.javaserver.config.ServerConfig;

public class ClientState {
    // For reading the incoming request
    public final ByteBuffer readBuffer = ByteBuffer.allocate(8192);
    public final StringBuilder rawRequest = new StringBuilder();

    // For writing the response
    public ByteBuffer writeBuffer = null;

    // Which server config owns this connection
    public final ServerConfig config;

    // Timeout tracking
    public long lastActivity = System.currentTimeMillis();

    public ClientState(ServerConfig config) {
        this.config = config;
    }

    public boolean hasCompleteRequest() {
        // HTTP headers end with \r\n\r\n
        return rawRequest.toString().contains("\r\n\r\n");
    }

    public void touch() {
        lastActivity = System.currentTimeMillis();
    }

    public boolean isTimedOut(long timeoutMs) {
        return System.currentTimeMillis() - lastActivity > timeoutMs;
    }
}