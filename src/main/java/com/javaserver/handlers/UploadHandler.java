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

public class UploadHandler {

    public static Response handle(Request request, ConfigRoute route, ConfigServer config) {

        // 1. Vérifier que c'est un POST
        if (!request.getMethod().equals("POST")) {
            return ErrorHandler.handle(405, config.getErrorPages());
        }

        // 2. Récupérer le body
        String body = request.getBody();
        if (body == null || body.isEmpty()) {
            return ErrorHandler.handle(400, config.getErrorPages());
        }

        // 3. Récupérer le nom du fichier depuis le header
        String contentType = request.getHeaders().getOrDefault("Content-Type", "");
        String fileName = extractFileName(contentType, body);

        if (fileName == null) {
            fileName = "upload_" + System.currentTimeMillis() + ".txt";
        }

        // 4. Créer le dossier d'upload si il n'existe pas
        Path uploadDir = Paths.get(route.getRoot());
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 5. Extraire le contenu du fichier depuis multipart
            byte[] fileContent = extractFileContent(contentType, body);

            // 6. Sauvegarder le fichier
            Path filePath = uploadDir.resolve(fileName);
            Files.write(filePath, fileContent);

            System.out.println("[UploadHandler] Fichier sauvegardé: " + filePath);

            // 7. Retourner 201 Created
            String responseBody = "<h1>Upload réussi: " + fileName + "</h1>";
            return new Response(201, "Created", "text/html", responseBody.getBytes());

        } catch (IOException e) {
            System.out.println("[UploadHandler] Erreur: " + e.getMessage());
            return ErrorHandler.handle(500, config.getErrorPages());
        }
    }

    // Extraire le nom du fichier depuis le header Content-Disposition
    private static String extractFileName(String contentType, String body) {
        if (body.contains("filename=")) {
            int start = body.indexOf("filename=\"") + 10;
            int end = body.indexOf("\"", start);
            if (start > 9 && end > start) {
                return body.substring(start, end);
            }
        }
        return null;
    }

    // Extraire le contenu du fichier depuis multipart/form-data
    private static byte[] extractFileContent(String contentType, String body) {
        // Trouver la boundary
        if (contentType.contains("boundary=")) {
            String boundary = "--" + contentType.split("boundary=")[1].trim();
            String[] parts = body.split(boundary);
            for (String part : parts) {
                if (part.contains("filename=")) {
                    // Sauter les headers de la partie
                    int contentStart = part.indexOf("\r\n\r\n");
                    if (contentStart != -1) {
                        String content = part.substring(contentStart + 4);
                        // Supprimer le \r\n final
                        content = content.replaceAll("\r\n--$", "").trim();
                        return content.getBytes();
                    }
                }
            }
        }
        return body.getBytes();
    }
}