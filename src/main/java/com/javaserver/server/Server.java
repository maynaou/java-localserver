package com.javaserver.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.List;

import com.javaserver.config.ConfigServer;

public class Server {

    private final List<ConfigServer> configs;
    private Selector selector;

    public Server(List<ConfigServer> configs) {
        this.configs = configs;
    }

    public void start() throws IOException {
        System.out.println("Démarrage du serveur avec la configuration:");
        for (ConfigServer config : configs) {
            System.out.println(config);
        }

        selector = Selector.open();

        // Ouvrir un canal par port pour chaque serveur configuré
        for (ConfigServer config : configs) {
            for (int port : config.getPorts()) {
                openChannel(config, port);
            }
        }

        System.out.println("[Server] Démarré — en attente de connexions...");

        // Passer la main à l'EventLoop — bloque ici jusqu'à stop()
        new EventLoop(selector).run();
    }


        // ── Ouverture d'un canal sur un port ──────────────────────────────────────

    private void openChannel(ConfigServer config, int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();

        // Non-bloquant : obligatoire pour NIO Selector
        channel.configureBlocking(false);

        // Lier le canal à l'adresse et au port
        channel.bind(new InetSocketAddress(config.getHost(), port));

        // Enregistrer dans le Selector :
        // OP_ACCEPT = on veut être notifié quand une connexion arrive
        // config     = attachée à la clé, récupérée dans EventLoop
        channel.register(selector, SelectionKey.OP_ACCEPT, config);

        System.out.println("[Server] Écoute sur " + config.getHost() + ":" + port);
    }

    // ── Arrêt propre ──────────────────────────────────────────────────────────

    public void stop() throws IOException {
        if (selector != null && selector.isOpen()) {
            selector.close();
        }
        System.out.println("[Server] Arrêté.");
    }
}