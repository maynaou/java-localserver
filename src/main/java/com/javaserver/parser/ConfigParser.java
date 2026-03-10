package com.javaserver.parser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.javaserver.config.ConfigRoute;
import com.javaserver.config.ConfigServer;

public class ConfigParser {
    @SuppressWarnings("unchecked")
    public static List<ConfigServer> parse(String filePath) {
        String json;
        try {
            json = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (Exception e) {
            throw new RuntimeException("[ConfigParser] Impossible de lire: " + filePath, e);
        }

        JsonParser parser = new JsonParser(json);
        Object root = parser.parse();
        Map<String, Object> rootMap = (Map<String, Object>) root;
        List<Object> serversArray = (List<Object>) rootMap.get("servers");
        System.out.println(serversArray);
        List<ConfigServer> result = new ArrayList<>();
        for (Object item : serversArray) {
            Map<String, Object> serverMap = (Map<String, Object>) item;
            result.add(buildServer(serverMap));
        }
        return result;
    }

@SuppressWarnings("unchecked")
private static ConfigServer buildServer(Map<String, Object> m) {

    // ── 1. CHAMPS SIMPLES ──────────────────────────────────────────
    // m.get() retourne un Object → on cast vers le vrai type
    String host            = (String)  m.get("host");           // "localhost"
    Boolean defaultServer  = (Boolean) m.get("default_server"); // true/false
    String clientBodyLimit = (String)  m.get("client_body_limit"); // "10MB"

    // ── 2. PORTS ───────────────────────────────────────────────────
    // JsonParser retourne TOUS les nombres comme Double → 8080.0
    // On doit convertir en Integer : 8080.0 → 8080
    List<Object> portsRaw = (List<Object>) m.get("ports");
    List<Integer> ports = new ArrayList<>();
    for (Object p : portsRaw) {
        ports.add(((Double) p).intValue()); // 8080.0 → 8080
    }

    // ── 3. ERROR PAGES ─────────────────────────────────────────────
    // Le JSON donne { "404": "error_pages/404.html", ... }
    // JsonParser produit Map<String, Object> → on recast les valeurs en String
    Map<String, Object> errorPagesRaw = (Map<String, Object>) m.get("error_pages");
    Map<String, String> errorPages = new HashMap<>();
    if (errorPagesRaw != null) {
        for (Map.Entry<String, Object> entry : errorPagesRaw.entrySet()) {
            errorPages.put(entry.getKey(), (String) entry.getValue());
        }
    }

    // ── 4. ROUTES ──────────────────────────────────────────────────
    // Chaque route est elle-même une Map → on délègue à buildRoute()
    List<Object> routesRaw = (List<Object>) m.get("routes");
    List<ConfigRoute> routes = new ArrayList<>();
    if (routesRaw != null) {
        for (Object r : routesRaw) {
            Map<String, Object> routeMap = (Map<String, Object>) r;
            routes.add(buildRoute(routeMap)); // même logique, pour une route
        }
    }

    // ── 5. ASSEMBLAGE ──────────────────────────────────────────────
    // Tous les champs sont prêts → on crée l'objet ConfigServer
    return new ConfigServer(host, ports, defaultServer, clientBodyLimit, errorPages, routes);
}

@SuppressWarnings("unchecked")
private static ConfigRoute buildRoute(Map<String, Object> m) {

    // ── 1. CHAMPS SIMPLES ──────────────────────────────────────────
    String path                 = (String)  m.get("path");
    String root                 = (String)  m.get("root");
    String defaultFile          = (String)  m.get("default_file");
    String defaultDirectoryFile = (String)  m.get("default_directory_file");

    // ── 2. DIRECTORY LISTING ───────────────────────────────────────
    // Peut être absent du JSON (ex: route /upload n'a pas ce champ)
    // → on met false par défaut pour éviter un NullPointerException
    Boolean directoryListing = (Boolean) m.get("directory_listing");
    if (directoryListing == null) directoryListing = false;

    // ── 3. METHODS ─────────────────────────────────────────────────
    // ["GET", "POST"] → JsonParser produit List<Object>
    // → on cast chaque élément en String
    List<Object> methodsRaw = (List<Object>) m.get("methods");
    List<String> methods = new ArrayList<>();
    if (methodsRaw != null) {
        for (Object method : methodsRaw) {
            methods.add((String) method); // "GET", "POST", "DELETE"...
        }
    }

    // ── 4. ASSEMBLAGE ──────────────────────────────────────────────
    // L'ordre des paramètres doit correspondre EXACTEMENT au constructeur :
    // ConfigRoute(path, methods, root, defaultFile, directoryListing, defaultDirectoryFile)
    return new ConfigRoute(path, methods, root, defaultFile, directoryListing, defaultDirectoryFile);
}


}
