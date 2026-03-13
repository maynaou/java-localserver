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
        String scriptPath = route.getRoot() + request.getPath().substring(route.getPath().length());
        System.out.println("[CGIHandler] Script: " + scriptPath);

        try {
            // 2. Lancer le script Python
            ProcessBuilder pb = new ProcessBuilder("python3", scriptPath);

            // 3. Variables d'environnement CGI standard
            Map<String, String> env = pb.environment();
            env.put("REQUEST_METHOD",  request.getMethod());
            env.put("PATH_INFO",       request.getPath());
            env.put("QUERY_STRING",    getQueryString(request.getPath()));
            env.put("CONTENT_LENGTH",  String.valueOf(request.getBody().length()));
            env.put("CONTENT_TYPE",    request.getHeaders().getOrDefault("Content-Type", ""));
            env.put("HTTP_HOST",       request.getHeaders().getOrDefault("Host", ""));

            pb.redirectErrorStream(true);
            Process process = pb.start();

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

            System.out.println("[CGIHandler] Output: " + output);
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