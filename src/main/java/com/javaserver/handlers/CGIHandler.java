package com.javaserver.handlers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;

import com.javaserver.config.ConfigRoute;
import com.javaserver.config.ConfigServer;
import com.javaserver.errors.ErrorHandler;
import com.javaserver.http.Request;
import com.javaserver.http.Response;

public class CGIHandler {

    public static Response handle(Request request, ConfigRoute route, ConfigServer config) {

        // 1. Construire le chemin du script
        // ex: route.path="/scripts", request.path="/scripts/hello.py"
        // → scriptPath = "cgi-bin/hello.py"
        String fullPath = request.getPath().substring(route.getPath().length());
        String scriptPath = route.getRoot() + fullPath.split("\\?")[0];
        System.out.println("[CGIHandler] Script: " + scriptPath);

        try {
            // 2. Lancer le script Python
            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath);

            // 3. ✅ FIX 4: Variables d'environnement CGI standard (RFC 3875)
            Map<String, String> env = pb.environment();
            
            // Méthode de la requête
            env.put("REQUEST_METHOD",  request.getMethod());
            
            // Path et query
            env.put("SCRIPT_NAME",     route.getPath());
            env.put("PATH_INFO",       request.getPath());
            env.put("QUERY_STRING",    getQueryString(request.getPath()));
            
            // Serveur info
            env.put("SERVER_NAME",       config.getHost());
            env.put("SERVER_PORT",       String.valueOf(config.getPorts().get(0)));
            env.put("SERVER_PROTOCOL",   "HTTP/1.1");
            env.put("SERVER_SOFTWARE",   "JavaServer/1.0");
            
            // Client info
            env.put("REMOTE_ADDR",       "127.0.0.1"); // TODO: récupérer depuis socket
            env.put("REMOTE_HOST",       "127.0.0.1");
            
            // Content
            env.put("CONTENT_LENGTH",  String.valueOf(request.getBody().length()));
            env.put("CONTENT_TYPE",    request.getHeaders().getOrDefault("Content-Type", ""));
            
            // HTTP headers (préfixé avec HTTP_)
            for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                String envKey = "HTTP_" + header.getKey().replace("-", "_").toUpperCase();
                env.put(envKey, header.getValue());
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // ✅ Si POST avec body, envoyer via stdin
            if (request.getMethod().equals("POST") && !request.getBody().isEmpty()) {
                process.getOutputStream().write(request.getBody().getBytes());
                process.getOutputStream().close();
            }

            // 4. Lire la sortie du script
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();

            System.out.println("[CGIHandler] Output: " + output.substring(0, Math.min(200, output.length())));
            return new Response(200, "OK", "text/html", output.toString().getBytes());

        } catch (Exception e) {
            System.out.println("[CGIHandler] Erreur: " + e.getMessage());
            return ErrorHandler.handle(500, config.getErrorPages());
        }
    }

    private static String getQueryString(String path) {
        int idx = path.indexOf('?');
        return (idx != -1) ? path.substring(idx + 1) : "";
    }
}