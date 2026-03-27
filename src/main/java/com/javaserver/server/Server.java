package com.javaserver.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.javaserver.config.ConfigServer;

public class Server {

    private final List<ConfigServer> configs;
    private Selector selector;

    public Server(List<ConfigServer> configs) {
        this.configs = configs;
    }

    public void start() throws IOException {
        System.out.println("Démarrage du serveur avec la configuration:");

        selector = Selector.open();

Map<Integer, List<ConfigServer>> portMap = new HashMap<>();
Set<String> seenHostPort = new HashSet<>();

for (ConfigServer config : configs) {
    for (int port : config.getPorts()) {
        String key = config.getHost() + ":" + port;
        if (!seenHostPort.add(key)) {
            throw new RuntimeException("[Server] Combinaison hôte:port dupliquée: " 
                + key + " — vérifiez votre config.json");
        }
        portMap.computeIfAbsent(port, k -> new ArrayList<>()).add(config);
    }
}
// ouvrir UN seul channel par port
for (Map.Entry<Integer, List<ConfigServer>> entry : portMap.entrySet()) {
    int port = entry.getKey();
    List<ConfigServer> configsForPort = entry.getValue();

    openChannel(port, configsForPort);
}

long portsOuverts = selector.keys().stream()
    .filter(k -> k.channel() instanceof ServerSocketChannel)
    .count();

if (portsOuverts == 0) {
    throw new RuntimeException("[Server] Aucun port disponible — impossible de démarrer.");
}

        System.out.println("[Server] Démarré — en attente de connexions...");

        // Passer la main à l'EventLoop — bloque ici jusqu'à stop()
        new EventLoop(selector).run();
    }


        // ── Ouverture d'un canal sur un port ──────────────────────────────────────

    private void openChannel(int port, List<ConfigServer> configs) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();

        // Non-bloquant : obligatoire pour NIO Selector
        channel.configureBlocking(false);

        // Lier le canal à l'adresse et au port
        try {
            channel.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            System.err.println("[Server] ⚠️ Port " + port + " déjà utilisé, ignoré: " + e.getMessage());
            channel.close();
            return; // ← ignorer ce port, continuer les autres
        }

        // Enregistrer dans le Selector :
        // OP_ACCEPT = on veut être notifié quand une connexion arrive
        // config     = attachée à la clé, récupérée dans EventLoop
        channel.register(selector, SelectionKey.OP_ACCEPT, configs);

        System.out.println("[Server] Écoute sur port " + port + " pour " + configs.size() + " serveur(s)");
    }

    // ── Arrêt propre ──────────────────────────────────────────────────────────

    public void stop() throws IOException {
        if (selector != null && selector.isOpen()) {
            selector.close();
        }
        System.out.println("[Server] Arrêté.");
    }
}