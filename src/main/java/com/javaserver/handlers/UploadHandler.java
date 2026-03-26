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
        String fileName = null;
        byte[] fileContent = null;

        // ✅ FIX 5: Améliorer le parsing multipart
        if (contentType.contains("multipart/form-data")) {
            fileName = extractFileName(contentType, body);
            fileContent = extractFileContent(contentType, body);
        } else if (contentType.contains("application/octet-stream") || 
                   contentType.contains("application/") ||
                   contentType.contains("text/")) {
            // Upload raw binary/text
            fileContent = body.getBytes();
            fileName = extractFileNameFromHeader(request);
        } else {
            // Pas de content-type valide
            return ErrorHandler.handle(400, config.getErrorPages());
        }

        if (fileName == null || fileName.isEmpty()) {
            fileName = "upload_" + System.currentTimeMillis() + ".bin";
        }

        if (fileContent == null || fileContent.length == 0) {
            return ErrorHandler.handle(400, config.getErrorPages());
        }

        // 4. Sécurité: vérifier le filename ne contient pas de path traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            fileName = "upload_" + System.currentTimeMillis() + ".bin";
        }

        // 5. Créer le dossier d'upload si il n'existe pas
        Path uploadDir = Paths.get(route.getRoot());
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // 6. Sauvegarder le fichier
            Path filePath = uploadDir.resolve(fileName);
            Files.write(filePath, fileContent);

            System.out.println("[UploadHandler] Fichier sauvegardé: " + filePath + " (" + fileContent.length + " bytes)");

            // 7. Retourner 201 Created
            String responseBody = "<h1>Upload réussi: " + fileName + "</h1><p>Taille: " + fileContent.length + " bytes</p>";
            return new Response(201, "Created", "text/html", responseBody.getBytes());

        } catch (IOException e) {
            System.out.println("[UploadHandler] Erreur: " + e.getMessage());
            return ErrorHandler.handle(500, config.getErrorPages());
        }
    }

    // Extraire le nom du fichier depuis Content-Disposition header
    private static String extractFileNameFromHeader(Request request) {
        String contentDisposition = request.getHeaders().get("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            int start = contentDisposition.indexOf("filename=") + 9;
            if (contentDisposition.charAt(start) == '"') {
                start++;
                int end = contentDisposition.indexOf("\"", start);
                if (end != -1) {
                    return contentDisposition.substring(start, end);
                }
            }
        }
        return null;
    }

    // Extraire le nom du fichier depuis le body multipart
    private static String extractFileName(String contentType, String body) {
        // Chercher filename= dans le body
        int pos = body.indexOf("filename=");
        if (pos != -1) {
            // Format: filename="name.txt" ou filename=name.txt
            int start = pos + 9;
            if (start < body.length() && body.charAt(start) == '"') {
                start++;
                int end = body.indexOf("\"", start);
                if (end != -1) {
                    return body.substring(start, end);
                }
            } else {
                // Pas de quotes
                int end = Math.min(body.indexOf("\r\n", start), body.indexOf(";", start));
                if (end == -1) end = Math.min(body.indexOf("\n", start), body.length());
                if (end > start) {
                    return body.substring(start, end).trim();
                }
            }
        }
        return null;
    }

    // Extraire le contenu du fichier depuis multipart/form-data
    private static byte[] extractFileContent(String contentType, String body) {
        // 1. Extraire la boundary
        String boundary = extractBoundary(contentType);
        if (boundary == null || boundary.isEmpty()) {
            return body.getBytes();
        }

        // 2. Chercher la part avec filename=
        String boundaryMarker = "--" + boundary;
        int filePartStart = body.indexOf("filename=");
        if (filePartStart == -1) {
            return body.getBytes();
        }

        // Trouver le début du contenu (après \r\n\r\n)
        int contentStart = body.indexOf("\r\n\r\n", filePartStart);
        if (contentStart == -1) {
            contentStart = body.indexOf("\n\n", filePartStart);
            if (contentStart != -1) contentStart += 2;
        } else {
            contentStart += 4;
        }

        if (contentStart == -1) return body.getBytes();

        // 3. Chercher la fin du contenu (prochaine boundary)
        int contentEnd = body.indexOf(boundaryMarker, contentStart);
        if (contentEnd == -1) {
            contentEnd = body.length();
        }

        // Supprimer les \r\n ou \n avant la boundary
        String content = body.substring(contentStart, contentEnd);
        while (content.endsWith("\r\n")) {
            content = content.substring(0, content.length() - 2);
        }
        while (content.endsWith("\n")) {
            content = content.substring(0, content.length() - 1);
        }

        return content.getBytes();
    }

    // Extraire la boundary depuis Content-Type
    private static String extractBoundary(String contentType) {
        int pos = contentType.indexOf("boundary=");
        if (pos != -1) {
            int start = pos + 9;
            // Boundary peut être quoted ou pas
            if (start < contentType.length() && contentType.charAt(start) == '"') {
                start++;
                int end = contentType.indexOf("\"", start);
                if (end != -1) {
                    return contentType.substring(start, end).trim();
                }
            } else {
                // Pas quoted, jusqu'à ; ou fin
                int end = contentType.indexOf(";", start);
                if (end == -1) end = contentType.length();
                return contentType.substring(start, end).trim();
            }
        }
        return null;
    }
}