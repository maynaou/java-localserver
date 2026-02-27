package com.javaserver.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

public class Server {
    private static final int PORT = 8080;

    public void start() {
        try {
            Selector selector = Selector.open();

            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(PORT));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("Server is listening on port " + PORT);

            while (true) {
                selector.select();
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();
                System.out.println("Number of events: " + selectedKeys.size());
                while (iterator.hasNext()) {
                    System.out.println("Processing a new event...");
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isAcceptable()) {
                        // New client connecting
                        ServerSocketChannel server = (ServerSocketChannel) key.channel();
                        SocketChannel client = server.accept();
                        client.configureBlocking(false);
                        client.register(selector, SelectionKey.OP_READ);
                        System.out.println("Client connected: " + client.getRemoteAddress());

                    } else if (key.isReadable()) {
                        // Client sent data
                        SocketChannel client = (SocketChannel) key.channel();
                        ByteBuffer buffer = ByteBuffer.allocate(1024);
                        int bytesRead = client.read(buffer);

                        if (bytesRead == -1) {
                            client.close();
                            key.cancel();
                            System.out.println("Client disconnected");
                            continue;
                        }



                        String request = new String(buffer.array(), 0, bytesRead);
                        System.out.println("--- Request Received ---\n" + request);
                        

                        // Minimal HTTP response so browser doesn't hang
                        String body = "<h1>Server is working!</h1>";
                        String response = "HTTP/1.1 200 OK\r\n"
                                + "Content-Type: text/html\r\n"
                                + "Content-Length: " + body.length() + "\r\n"
                                + "Connection: close\r\n"
                                + "\r\n"
                                + body;

                        client.write(ByteBuffer.wrap(response.getBytes()));
                        client.close();
                        key.cancel();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}