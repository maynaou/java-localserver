package com.javaserver.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.List;

import com.javaserver.config.ConfigServer;
import com.javaserver.errors.ErrorHandler;
import com.javaserver.http.Request;
import com.javaserver.http.Response;
import com.javaserver.http.Router;

public class ClientConnection {

    private static final int BUFFER_SIZE = 8192; // 8 KB — taille standard HTTP

    private final SocketChannel channel;
    private Request lastRequest;
    private final List<ConfigServer> configs;
    private final ByteBuffer buffer;
    private ByteBuffer writeBuffer;  // buffer pour écrire la réponse (null si rien à écrire)
    private long lastActivity = System.currentTimeMillis();
    
    // ✅ FIX 2: Accumulation du request body pour les gros uploads (en bytes!)
    private java.io.ByteArrayOutputStream requestBytes = new java.io.ByteArrayOutputStream();
    private int expectedContentLength = 0;
    private boolean headersParsed = false;


    public ClientConnection(SocketChannel channel, List<ConfigServer> configs) {
        this.channel = channel;
        this.configs  = configs;
        this.buffer  = ByteBuffer.allocate(BUFFER_SIZE);
        this.writeBuffer = null;
    }

    // ── Lecture des données brutes ────────────────────────────────────────────

    public void read(SelectionKey key) throws IOException {
        lastActivity = System.currentTimeMillis();
        buffer.clear();
        int bytesRead = channel.read(buffer);

        if (bytesRead == -1) {
            close(key);
            return;
        }

        if (bytesRead == 0) return;

        // Ajouter les données lues à l'accumulateur
        buffer.flip();
        byte[] data = new byte[buffer.limit()];
        buffer.get(data);
        requestBytes.write(data);

        byte[] allBytes = requestBytes.toByteArray();

        // ✅ FIX 2: Traiter headers et body séparément
        // Les headers sont ASCII/UTF-8, mais le body peut être binaire
        
        if (!headersParsed) {
            // Chercher la fin des headers (séquence \r\n\r\n en bytes)
            int headerEnd = findBytes(allBytes, "\r\n\r\n".getBytes());
            if (headerEnd == -1) {
                // Headers incomplets, attendre
                return;
            }
            
            headersParsed = true;
            
            // Parser headers comme string (jusqu'au séparateur)
            String headerStr = new String(allBytes, 0, headerEnd);
            String[] lines = headerStr.split("\r\n");
            for (String line : lines) {
                if (line.toLowerCase().startsWith("content-length:")) {
                    try {
                        expectedContentLength = Integer.parseInt(line.substring(15).trim());
                        System.out.println("[ClientConnection] Content-Length: " + expectedContentLength);
                    } catch (NumberFormatException e) {
                        System.out.println("[ClientConnection] Content-Length invalide");
                    }
                }
            }
        }

        // ✅ Vérifier si on a reçu TOUS les bytes du body
        int headerEnd = findBytes(allBytes, "\r\n\r\n".getBytes());
        if (headerEnd != -1) {
            int bodyStart = headerEnd + 4;
            int totalBodyLength = allBytes.length - bodyStart;
            
            // Attendre que le body soit complet
            if (totalBodyLength < expectedContentLength) {
                return;  // Continuer à lire
            }
        }

        // ✅ Maintenant on a toute la requête
        String fullRequest = new String(allBytes);
        System.out.println("[ClientConnection] Requête complète: headers+" + expectedContentLength + " body bytes");

        // Reset pour la prochaine requête (keep-alive)
        requestBytes = new java.io.ByteArrayOutputStream();
        headersParsed = false;
        expectedContentLength = 0;

        // Parser la requête
        try {
            lastRequest = Request.parse(fullRequest);
        } catch (Exception e) {
            System.out.println("[ClientConnection] Requête malformée: " + e.getMessage());
            ConfigServer config = getDefaultConfig();
            Response r = ErrorHandler.handle(400, config.getErrorPages());
            writeBuffer = ByteBuffer.wrap(r.toBytes());
            key.interestOps(SelectionKey.OP_WRITE);
            return;
        }

        // ✅ Sélectionner le bon ConfigServer basé sur le header Host
        ConfigServer config = findConfigByHost(lastRequest);
        if (config == null) {
            config = getDefaultConfig();
            if (config == null) {
                System.out.println("[ClientConnection] Aucun serveur configuré!");
                close(key);
                return;
            }
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
                    Response r = ErrorHandler.handle(400, config.getErrorPages());
                    writeBuffer = ByteBuffer.wrap(r.toBytes());
                    key.interestOps(SelectionKey.OP_WRITE);
                    return;
                }
            }
        }
        Response response = Router.handle(lastRequest, config);

        // ✅ Keep-Alive support: respect Client's Connection header
        String connectionHeader = lastRequest.getHeaders().get("Connection");
        if ("keep-alive".equalsIgnoreCase(connectionHeader)) {
            response.setKeepAlive(true);
        }

        writeBuffer = ByteBuffer.wrap(response.toBytes());
        key.interestOps(SelectionKey.OP_WRITE);
    }

    // ✅ Helper: trouver un pattern de bytes
    private int findBytes(byte[] haystack, byte[] needle) {
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    found = false;
                    break;
                }
            }
            if (found) {
                return i;
            }
        }
        return -1;
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

    // ── Virtual Hosting Helper ─────────────────────────────────────────────────

    /**
     * Sélectionne le ConfigServer approprié en fonction du header Host.
     * Support du virtual hosting.
     */
    private ConfigServer findConfigByHost(Request request) {
        String hostHeader = request.getHeaders().get("Host");
        if (hostHeader == null || hostHeader.isEmpty()) {
            return null;
        }

        // Format: "hostname" ou "hostname:port"
        String hostname = hostHeader.split(":")[0].trim();
        System.out.println("[ClientConnection] Host header: " + hostHeader + " → hostname: " + hostname);

        // Chercher le ConfigServer qui correspond à ce hostname
        for (ConfigServer config : configs) {
            if (config.getHost() != null && config.getHost().equalsIgnoreCase(hostname)) {
                System.out.println("[ClientConnection] ✅ Trouvé ConfigServer pour: " + hostname);
                return config;
            }
        }

        System.out.println("[ClientConnection] ⚠️ Aucun ConfigServer pour: " + hostname);
        return null;
    }

    /**
     * Retourner le ConfigServer par défaut (marked as default).
     */
    private ConfigServer getDefaultConfig() {
        // Chercher un serveur marqué comme "default"
        for (ConfigServer config : configs) {
            if (config.isDefaultServer()) {
                System.out.println("[ClientConnection] Utilisant le serveur par défaut: " + config.getHost());
                return config;
            }
        }

        // Sinon, prendre le premier
        if (!configs.isEmpty()) {
            System.out.println("[ClientConnection] Pas de serveur par défaut, utilisant le premier: " + configs.get(0).getHost());
            return configs.get(0);
        }

        return null;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public SocketChannel getChannel() { return channel; }
    public List<ConfigServer> getConfigs()   { return configs;  }
    public long getLastActivity() { return lastActivity; }
}