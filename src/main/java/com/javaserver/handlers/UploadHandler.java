package com.javaserver.handlers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import com.javaserver.config.ConfigRoute;
import com.javaserver.config.ConfigServer;
import com.javaserver.errors.ErrorHandler;
import com.javaserver.http.Request;
import com.javaserver.http.Response;

public class UploadHandler {

    public static Response handle(Request request, ConfigRoute route, ConfigServer config) {

        if (!request.getMethod().equals("POST")) {
            return ErrorHandler.handle(405, config.getErrorPages());
        }

        byte[] fileContent = null;
        String fileName = null;
        String contentType = request.getHeaders().getOrDefault("Content-Type", "");

        if (contentType.contains("multipart/form-data")) {
            // ✅ utiliser rawBody (bytes bruts) pour ne pas corrompre le binaire
            fileName = extractFileName(contentType, request.getBody());
            fileContent = extractFileContentBytes(contentType, request.getRawBody());
        } else if (contentType.contains("application/") || contentType.contains("text/")) {
            // ✅ upload raw — bytes bruts directement
            fileContent = request.getRawBody();
            fileName = extractFileNameFromHeader(request);
        } else {
            return ErrorHandler.handle(400, config.getErrorPages());
        }

        if (fileName == null || fileName.isEmpty()) {
            fileName = "upload_" + System.currentTimeMillis() + ".bin";
        }

        if (fileContent == null || fileContent.length == 0) {
            return ErrorHandler.handle(400, config.getErrorPages());
        }

        // Sécurité path traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            fileName = "upload_" + System.currentTimeMillis() + ".bin";
        }

        Path uploadDir = Paths.get(route.getRoot());
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            Path filePath = uploadDir.resolve(fileName);
            Files.write(filePath, fileContent); // ✅ écriture bytes bruts

            System.out.println("[UploadHandler] Fichier sauvegardé: " + filePath + " (" + fileContent.length + " bytes)");

            String responseBody = "<h1>Upload réussi: " + fileName + "</h1><p>Taille: " + fileContent.length + " bytes</p>";
            return new Response(201, "Created", "text/html", responseBody.getBytes());

        } catch (IOException e) {
            System.out.println("[UploadHandler] Erreur: " + e.getMessage());
            return ErrorHandler.handle(500, config.getErrorPages());
        }
    }

    // ✅ Extraction bytes bruts depuis multipart (pas de conversion String)
    private static byte[] extractFileContentBytes(String contentType, byte[] rawBody) {
        String boundary = extractBoundary(contentType);
        if (boundary == null) return rawBody;

        byte[] headerSep = "\r\n\r\n".getBytes();
        byte[] endBoundary = ("\r\n--" + boundary).getBytes();

        int contentStart = findBytes(rawBody, headerSep);
        if (contentStart == -1) return rawBody;
        contentStart += 4;

        int contentEnd = findBytes(rawBody, endBoundary);
        if (contentEnd == -1) contentEnd = rawBody.length;

        return Arrays.copyOfRange(rawBody, contentStart, contentEnd);
    }

    private static String extractFileNameFromHeader(Request request) {
        String contentDisposition = request.getHeaders().get("Content-Disposition");
        if (contentDisposition != null && contentDisposition.contains("filename=")) {
            int start = contentDisposition.indexOf("filename=") + 9;
            if (contentDisposition.charAt(start) == '"') {
                start++;
                int end = contentDisposition.indexOf("\"", start);
                if (end != -1) return contentDisposition.substring(start, end);
            }
        }
        return null;
    }

    private static String extractFileName(String contentType, String body) {
        int pos = body.indexOf("filename=");
        if (pos != -1) {
            int start = pos + 9;
            if (start < body.length() && body.charAt(start) == '"') {
                start++;
                int end = body.indexOf("\"", start);
                if (end != -1) return body.substring(start, end);
            }
        }
        return null;
    }

    private static String extractBoundary(String contentType) {
        int pos = contentType.indexOf("boundary=");
        if (pos != -1) {
            int start = pos + 9;
            if (start < contentType.length() && contentType.charAt(start) == '"') {
                start++;
                int end = contentType.indexOf("\"", start);
                if (end != -1) return contentType.substring(start, end).trim();
            } else {
                int end = contentType.indexOf(";", start);
                if (end == -1) end = contentType.length();
                return contentType.substring(start, end).trim();
            }
        }
        return null;
    }

    private static int findBytes(byte[] haystack, byte[] needle) {
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            boolean found = true;
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) { found = false; break; }
            }
            if (found) return i;
        }
        return -1;
    }
}