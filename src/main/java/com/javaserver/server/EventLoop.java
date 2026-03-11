package com.javaserver.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

import com.javaserver.config.ConfigServer;

public class EventLoop {

    private final Selector selector;

    public EventLoop(Selector selector) {
        this.selector = selector;
    }

    // ── Boucle principale ─────────────────────────────────────────────────────

    public void run() {
        while (true) {
            try {
                // Bloque jusqu'à ce qu'au moins un canal soit prêt
                selector.select();

                Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove(); // toujours retirer après traitement

                    if (!key.isValid()) continue;

                    if (key.isAcceptable()) {
                        acceptConnection(key); // nouvelle connexion entrante
                    } else if (key.isReadable()) {
                        readRequest(key);      // données à lire depuis un client
                    } else if (key.isWritable()) {
                        // TODO — écrire la réponse au client
                        writeResponse(key);    // on peut envoyer la réponse  ← nouveau
                    }
                }

            } catch (IOException e) {
                // On log l'erreur mais on NE crash PAS
                // Le serveur doit tourner en continu (exigence du sujet)
                System.err.println("[EventLoop] Erreur: " + e.getMessage());
            }
        }
    }

    // ── Accepter une nouvelle connexion ───────────────────────────────────────

    private void acceptConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();

        // Accepter le client — peut retourner null en mode non-bloquant
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel == null) return;

        clientChannel.configureBlocking(false);

        // Récupérer la config attachée à ce canal serveur
        ConfigServer config = (ConfigServer) key.attachment();

        // Créer un objet qui représente ce client
        ClientConnection connection = new ClientConnection(clientChannel, config);

        // Enregistrer ce client dans le Selector :
        // OP_READ = on veut être notifié quand il envoie des données
        clientChannel.register(selector, SelectionKey.OP_READ, connection);

        System.out.println("[EventLoop] Nouvelle connexion: " + clientChannel.getRemoteAddress());
    }

    // ── Lire les données d'un client ──────────────────────────────────────────

    private void readRequest(SelectionKey key) {
        ClientConnection connection = (ClientConnection) key.attachment();
        try {
            connection.read(key);
        } catch (IOException e) {
            // Erreur sur UN client → fermer SA connexion, pas le serveur entier
            System.err.println("[EventLoop] Erreur lecture client: " + e.getMessage());
            tryClose(connection, key);
        }
    }

        // ── Écrire une réponse ────────────────────────────────────────────────────

    private void writeResponse(SelectionKey key) {
        ClientConnection connection = (ClientConnection) key.attachment();
        try {
            connection.write(key);
            // Après write(), si tout est envoyé,
            // ClientConnection a désactivé OP_WRITE automatiquement
        } catch (IOException e) {
            System.err.println("[EventLoop] Erreur écriture: " + e.getMessage());
            tryClose(connection, key);
        }
    }


    private void tryClose(ClientConnection connection, SelectionKey key) {
        try {
            connection.close(key);
        } catch (IOException ignored) {}
    }

}