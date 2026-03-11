package com.javaserver.http;

import com.javaserver.config.ConfigRoute;
import com.javaserver.config.ConfigServer;
import com.javaserver.errors.ErrorHandler;
import com.javaserver.handlers.StaticHandler;

public class Router {

    public static Response handle(Request request, ConfigServer config) {

        // 1. Trouver la route qui correspond au path
        ConfigRoute route = findRoute(request.getPath(), config);

        if (route == null) {
            // Aucune route trouvée → 404
           return ErrorHandler.handle(404, config.getErrorPages());        }

        // 2. Vérifier que la méthode est autorisée
        if (!route.getMethods().contains(request.getMethod())) {
            return ErrorHandler.handle(405, config.getErrorPages());
        }

        // 3. Déléguer au bon handler (pour l'instant juste statique)
        return StaticHandler.handle(request, route, config);
    }

    private static ConfigRoute findRoute(String path, ConfigServer config) {
        ConfigRoute bestMatch = null;

        for (ConfigRoute route : config.getRoutes()) {
            if (path.startsWith(route.getPath())) {
                // Prendre la route la plus précise (la plus longue)
                if (bestMatch == null || route.getPath().length() > bestMatch.getPath().length()) {
                    bestMatch = route;
                }
            }
        }

        return bestMatch;
    }
}