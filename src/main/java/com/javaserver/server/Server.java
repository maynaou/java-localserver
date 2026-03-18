package com.javaserver.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import com.javaserver.utils.Cookie;
import com.javaserver.utils.Session;

import com.javaserver.config.Config;
import com.javaserver.config.ServerConfig;
import com.javaserver.errors.ErrorHandler;
import com.javaserver.http.HttpRequest;
import com.javaserver.http.HttpResponse;
import com.javaserver.http.Router;

public class Server {

    private static final long TIMEOUT_MS = 30_000; // 30 seconds

    private final Selector selector;
    private final Config config;

    public Server(Config config) throws IOException {
        this.selector = Selector.open();
        this.config = config;
    }

    public void start() {
        try {
            // --- Open one ServerSocketChannel per port per server ---
            for (ServerConfig cfg : config.getServers()) {
                for (int port : cfg.getPorts()) {
                    ServerSocketChannel serverChannel = ServerSocketChannel.open();
                    serverChannel.configureBlocking(false);
                    serverChannel.bind(new InetSocketAddress(cfg.getHost(), port));

                    // Attach the ServerConfig so we know which config owns new connections
                    SelectionKey key = serverChannel.register(selector, SelectionKey.OP_ACCEPT);
                    key.attach(cfg);

                    System.out.println("Listening on " + cfg.getHost() + ":" + port);
                }
            }

            System.out.println("Server started. Waiting for connections...");

            // --- Main event loop ---
            while (true) {
                // Timeout on select so we can run our cleanup check
                selector.select(5000);

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    try {
                        if (key.isValid() && key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isValid() && key.isReadable()) {
                            handleRead(key);
                        } else if (key.isValid() && key.isWritable()) {
                            handleWrite(key);
                        }
                    } catch (Exception e) {
                        // Never crash — log and close this one client
                        System.err.println("Error on key: " + e.getMessage());
                        closeKey(key);
                    }
                }

                // Cleanup timed-out connections
                checkTimeouts();
            }

        } catch (IOException e) {
            System.err.println("Fatal server error: " + e.getMessage());
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel client = serverChannel.accept();
        if (client == null)
            return;

        client.configureBlocking(false);

        // The ServerConfig that owns this listening port
        ServerConfig cfg = (ServerConfig) key.attachment();

        // Register client for reading, attach its state
        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ);
        clientKey.attach(new ClientState(cfg));

        System.out.println("Accepted connection from " + client.getRemoteAddress()
                + " on config: " + cfg.getHost());
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();
        state.touch();

        state.readBuffer.clear();
        int bytesRead = client.read(state.readBuffer);

        if (bytesRead == -1) {
            // Client closed connection
            closeKey(key);
            return;
        }

        // Append what we read into the accumulation buffer
        state.rawRequest.append(new String(state.readBuffer.array(), 0, bytesRead));

        // Check if we have a complete HTTP request (headers fully received)
        if (state.hasCompleteRequest()) {
            String rawReq = state.rawRequest.toString();

            HttpResponse response;
            try {
                ;
                HttpRequest request = HttpRequest.parse(rawReq);
                System.out.println("Request: " + request.getMethod() + " " + request.getPath()
                        + " [config: " + state.config.getHost() + "]");
                Map<String, String> cookies = Cookie.parseCookieHeader(
                        request.getHeader("cookie"));
                boolean isNewSession = !cookies.containsKey(Session.getCookieName());
                String sessionId = Session.getOrCreate(cookies);
                Router router = new Router(state.config);
                response = router.handle(request);
                if (isNewSession) {
                    response.setHeader("Set-Cookie", Session.buildCookieHeader(sessionId));
                }

            } catch (HttpRequest.HttpParseException e) {
                response = ErrorHandler.handle(e.statusCode, state.config.getErrorPages());
            }

            state.writeBuffer = ByteBuffer.wrap(response.toBytes());
            key.interestOps(SelectionKey.OP_WRITE);
        }
    }

    private void handleWrite(SelectionKey key) throws IOException {
        SocketChannel client = (SocketChannel) key.channel();
        ClientState state = (ClientState) key.attachment();
        state.touch();

        client.write(state.writeBuffer);

        if (!state.writeBuffer.hasRemaining()) {
            // All bytes sent — close connection
            closeKey(key);
        }
    }

    private void closeKey(SelectionKey key) {
        key.cancel();
        try {
            key.channel().close();
        } catch (IOException e) {
            System.err.println("Error closing channel: " + e.getMessage());
        }
    }

    private void checkTimeouts() {
        for (SelectionKey key : selector.keys()) {
            if (key.attachment() instanceof ClientState) {
                ClientState state = (ClientState) key.attachment();
                if (state.isTimedOut(TIMEOUT_MS)) {
                    System.out.println("Closing timed-out connection");
                    closeKey(key);
                }
            }
        }
    }
}