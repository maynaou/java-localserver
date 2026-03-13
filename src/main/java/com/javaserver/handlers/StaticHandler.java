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
import com.javaserver.utils.MimeTypes;

public class StaticHandler {

    public static Response handle(Request request, ConfigRoute route, ConfigServer config) {

        // 1. Construire le chemin du fichier
        // ex: root="www", path="/index.html" → "www/index.html"
        String filePath = route.getRoot() + request.getPath();

        Path path = Paths.get(filePath);

        System.out.println("[ErrorHandler] Cherche: " + path.toAbsolutePath());


        // 2. Si c'est un dossier → chercher le fichier par défaut
        if (Files.isDirectory(path)) {
            if (route.getDefaultFile() != null) {
                path = Paths.get(filePath + "/" + route.getDefaultFile());
            } else if (route.getDefaultDirectoryFile() != null) {
                path = Paths.get(filePath + "/" + route.getDefaultDirectoryFile());
            }
        }

        // 3. Fichier introuvable → 404
    if (!Files.exists(path)) {
        return ErrorHandler.handle(404, config.getErrorPages());
    }

        // 4. Lire le fichier et retourner son contenu
        try {
            byte[] body = Files.readAllBytes(path);
            String contentType = MimeTypes.getMimeType(path.toString());
            return new Response(200, "OK", contentType, body);

        } catch (IOException e) {
            return new Response(500, "Internal Server Error", "text/html", "<h1>500 Internal Server Error</h1>".getBytes());
        }
    }
}