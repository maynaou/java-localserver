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

        String relativePath = request.getPath().substring(route.getPath().length());
        if (!relativePath.startsWith("/")) relativePath = "/" + relativePath;
        String filePath = route.getRoot() + relativePath;
        Path path = Paths.get(filePath);

        // 2. Si c'est un dossier
        if (Files.isDirectory(path)) {

            // ✅ Directory listing activé → générer la liste
            if (route.isDirectoryListing()) {
                return generateListing(path, request.getPath());
            }

            // Directory listing désactivé → chercher fichier par défaut
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

        // 4. Lire le fichier
        try {
            byte[] body = Files.readAllBytes(path);
            String contentType = MimeTypes.getMimeType(path.toString());
            return new Response(200, "OK", contentType, body);
        } catch (IOException e) {
            return ErrorHandler.handle(500, config.getErrorPages());
        }
    }

    // ✅ Générer le listing HTML du dossier
    private static Response generateListing(Path dir, String urlPath) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><body>");
            html.append("<h1>Index of ").append(urlPath).append("</h1>");
            html.append("<ul>");

            Files.list(dir).forEach(file -> {
                String name = file.getFileName().toString();
                if (Files.isDirectory(file)) name += "/";
                html.append("<li><a href=\"")
                    .append(urlPath.endsWith("/") ? urlPath : urlPath + "/")
                    .append(name).append("\">")
                    .append(name).append("</a></li>");
            });

            html.append("</ul></body></html>");
            return new Response(200, "OK", "text/html", html.toString().getBytes());

        } catch (IOException e) {
            return new Response(500, "Internal Server Error", "text/html",
                "<h1>500 Internal Server Error</h1>".getBytes());
        }
    }
}