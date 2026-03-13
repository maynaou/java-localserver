package com.javaserver.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.javaserver.config.ConfigServer;
import com.javaserver.errors.ErrorHandler;
import com.javaserver.http.Request;
import com.javaserver.http.Response;
import com.javaserver.http.Router;

public class ClientConnection {

    private static final int BUFFER_SIZE = 8192; // 8 KB — taille standard HTTP

    private final SocketChannel channel;
    private Request lastRequest;
    private final ConfigServer config;
    private final ByteBuffer buffer;
    private ByteBuffer writeBuffer;  // buffer pour écrire la réponse (null si rien à écrire)
    private long lastActivity = System.currentTimeMillis();


    public ClientConnection(SocketChannel channel, ConfigServer config) {
        this.channel = channel;
        this.config  = config;
        this.buffer  = ByteBuffer.allocate(BUFFER_SIZE);
        this.writeBuffer = null;
    }

    // ── Lecture des données brutes ────────────────────────────────────────────

    public void read(SelectionKey key) throws IOException {
        lastActivity = System.currentTimeMillis(); // ✅ mettre à jour
        buffer.clear();
        int bytesRead = channel.read(buffer);

        if (bytesRead == -1) {
            // Le client a fermé la connexion proprement
            close(key);
            return;
        }

        if (bytesRead == 0) return; // rien à lire pour l'instant

        // Préparer le buffer pour la lecture
        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);
        String rawRequest = new String(data);

        System.out.println("[ClientConnection] Reçu:\n" + rawRequest);

        // TODO — prochaine étape :
        // HttpRequest request = HttpRequestParser.parse(rawRequest);
        // HttpResponse response = Router.handle(request, config);
        // channel.write(ByteBuffer.wrap(response.toBytes()));
               // ✅ Fix 3 — Bad request → 400
        try {
            lastRequest = Request.parse(rawRequest);
        } catch (Exception e) {
            System.out.println("[ClientConnection] Requête malformée: " + e.getMessage());
            Response r = ErrorHandler.handle(400, config.getErrorPages());
            writeBuffer = ByteBuffer.wrap(r.toBytes());
            key.interestOps(SelectionKey.OP_WRITE);
            return;
        }

if (lastRequest.getPath().contains("..")) {
    Response r = ErrorHandler.handle(403, config.getErrorPages());
    writeBuffer = ByteBuffer.wrap(r.toBytes());
    key.interestOps(SelectionKey.OP_WRITE);
    return;
}

 // ✅ Fix 4 — Content-Length invalide → 400, dépassé → 413
        String limitStr = config.getClientBodyLimit();
        if (limitStr != null) {
            long maxBytes = parseLimit(limitStr);
            String contentLengthStr = lastRequest.getHeaders().get("Content-Length");
            if (contentLengthStr != null) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr.trim());
                    if (contentLength > maxBytes) {
                        Response r = ErrorHandler.handle(413, config.getErrorPages());
                        writeBuffer = ByteBuffer.wrap(r.toBytes());
                        key.interestOps(SelectionKey.OP_WRITE);
                        return;
                    }
                } catch (NumberFormatException e) {
                    // ✅ Content-Length pas un nombre → 400
                    Response r = ErrorHandler.handle(400, config.getErrorPages());
                    writeBuffer = ByteBuffer.wrap(r.toBytes());
                    key.interestOps(SelectionKey.OP_WRITE);
                    return;
                }
            }
        }
        Response response = Router.handle(lastRequest, config);

        writeBuffer = ByteBuffer.wrap(response.toBytes());
        key.interestOps(SelectionKey.OP_WRITE);
    }


   public void write(SelectionKey key) throws IOException {
    if (writeBuffer == null) return;

    channel.write(writeBuffer);

    if (!writeBuffer.hasRemaining()) {
        writeBuffer = null;

        System.out.println("[ClientConnection] Réponse envoyée complètement.");

        // ✅ Vérifier si le client veut garder la connexion ouverte
        // ✅ Vérifier lastRequest null
String connection = (lastRequest != null) 
    ? lastRequest.getHeaders().getOrDefault("Connection", "close")
    : "close";
        if (connection.equalsIgnoreCase("keep-alive")) {
            // Garder la connexion ouverte — attendre la prochaine requête
            key.interestOps(SelectionKey.OP_READ);
        } else {
            // Fermer la connexion
            close(key);
        }
    } else {
        System.out.println("[ClientConnection] Écriture partielle — "
            + writeBuffer.remaining() + " bytes restants.");
    }
}


    // ── Fermeture propre ──────────────────────────────────────────────────────

    public void close(SelectionKey key) throws IOException {
        key.cancel();
        channel.close();
        System.out.println("[ClientConnection] Connexion fermée.");
    }

    private long parseLimit(String limit) {
    limit = limit.trim().toUpperCase();
    if (limit.endsWith("MB")) {
        return Long.parseLong(limit.replace("MB", "").trim()) * 1024 * 1024;
    } else if (limit.endsWith("KB")) {
        return Long.parseLong(limit.replace("KB", "").trim()) * 1024;
    } else if (limit.endsWith("GB")) {
        return Long.parseLong(limit.replace("GB", "").trim()) * 1024 * 1024 * 1024;
    }
    return Long.parseLong(limit);
}

    // ── Getters ───────────────────────────────────────────────────────────────

    public SocketChannel getChannel() { return channel; }
    public ConfigServer getConfig()   { return config;  }
    public long getLastActivity() { return lastActivity; }
}