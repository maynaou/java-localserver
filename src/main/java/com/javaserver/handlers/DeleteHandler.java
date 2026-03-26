package com.javaserver.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.javaserver.config.ConfigRoute;
import com.javaserver.config.ConfigServer;
import com.javaserver.errors.ErrorHandler;
import com.javaserver.http.Request;
import com.javaserver.http.Response;

public class DeleteHandler {

    public static Response handle(Request request, ConfigRoute route, ConfigServer config) {

        // 1. Construire le chemin du fichier
        String relativePath = request.getPath().substring(route.getPath().length());
        String filePath = route.getRoot() + relativePath;
        Path path = Paths.get(filePath);

        System.out.println("[DeleteHandler] Supprimer: " + path.toAbsolutePath());

        // ✅ FIX 3: Sécurité path traversal
        try {
            path = path.toRealPath();
            Path routeRoot = Paths.get(route.getRoot()).toRealPath();
            
            if (!path.startsWith(routeRoot)) {
                System.out.println("[DeleteHandler] ⚠️ Path traversal attempt: " + filePath);
                return ErrorHandler.handle(403, config.getErrorPages());
            }
        } catch (IOException e) {
            // Fichier n'existe peut-être pas, c'est OK
        }

        // 2. Fichier introuvable → 404
        if (!Files.exists(path)) {
            return ErrorHandler.handle(404, config.getErrorPages());
        }

        // 3. Ne pas supprimer un dossier
        if (Files.isDirectory(path)) {
            return ErrorHandler.handle(403, config.getErrorPages());
        }

        // 4. Supprimer le fichier
        try {
            Files.delete(path);
            System.out.println("[DeleteHandler] Fichier supprimé: " + filePath);
            String body = "<h1>Fichier supprimé: " + request.getPath() + "</h1>";
            return new Response(200, "OK", "text/html", body.getBytes());

        } catch (IOException e) {
            System.out.println("[DeleteHandler] Erreur: " + e.getMessage());
            return ErrorHandler.handle(500, config.getErrorPages());
        }
    }
}