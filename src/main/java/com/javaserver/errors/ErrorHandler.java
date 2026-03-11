package com.javaserver.errors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.javaserver.http.Response;

public class ErrorHandler {

    public static Response handle(int code, Map<String, String> errorPages) {
        String message = getMessage(code);

        System.out.println("[ErrorHandler] code=" + code + " errorPages=" + errorPages);


        // Chercher la page d'erreur personnalisée dans config.json
        if (errorPages != null && errorPages.containsKey(String.valueOf(code))) {
            String filePath = errorPages.get(String.valueOf(code));
            Path path = Paths.get(filePath);

            System.out.println("[ErrorHandler] chemin absolu: " + path.toAbsolutePath());
            System.out.println("[ErrorHandler] fichier existe: " + Files.exists(path));

            if (Files.exists(path)) {
                try {
                    byte[] body = Files.readAllBytes(path);
                    return new Response(code, message, "text/html", body);
                } catch (Exception e) {
                    // Si lecture échoue → réponse par défaut
                }
            }
        }

        // Page d'erreur par défaut (si pas de fichier configuré)
        byte[] body = ("<h1>" + code + " " + message + "</h1>").getBytes();
        return new Response(code, message, "text/html", body);
    }

    private static String getMessage(int code) {
        switch (code) {
            case 400: return "Bad Request";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 413: return "Content Too Large";
            case 500: return "Internal Server Error";
            default:  return "Error";
        }
    }
}