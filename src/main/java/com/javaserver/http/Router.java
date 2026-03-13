package com.javaserver.http;

import java.util.HashMap;
import java.util.Map;

import com.javaserver.config.ConfigRedirect;
import com.javaserver.config.ConfigRoute;
import com.javaserver.config.ConfigServer;
import com.javaserver.errors.ErrorHandler;
import com.javaserver.handlers.CGIHandler;
import com.javaserver.handlers.DeleteHandler;
import com.javaserver.handlers.StaticHandler;
import com.javaserver.handlers.UploadHandler;
import com.javaserver.utils.Cookie;
import com.javaserver.utils.Session;

public class Router {

    private static final Map<String, Session> sessions = new HashMap<>();


       public static Response handle(Request request, ConfigServer config) {

        // 1. Lire le cookie SID
        String cookieHeader = request.getHeaders().getOrDefault("Cookie", "");
        String sid = Cookie.get(cookieHeader, "SID");

        // 2. Récupérer ou créer une session
        Session session = null;
        if (sid != null) {
            session = sessions.get(sid);
        }
        if (session == null) {
            session = new Session();
            sessions.put(session.getId(), session);
        }

        // 3. Trouver la route
        ConfigRoute route = findRoute(request.getPath(), config);
        System.out.println("[Router] path=" + request.getPath() + " → route=" + (route != null ? route.getPath() : "null"));

        if (route == null) {
            return ErrorHandler.handle(404, config.getErrorPages());
        }

        // 4. Vérifier la méthode
        if (!route.getMethods().contains(request.getMethod())) {
            return ErrorHandler.handle(405, config.getErrorPages());
        }

        if (route.getRedirects() != null && !route.getRedirects().isEmpty()) {
             ConfigRedirect redirect = route.getRedirects().get(0);
             Response r = new Response(redirect.getCode(), "Moved", "text/html", "".getBytes());
             r.addHeader("Location", redirect.getTarget());
            return r;
        }

// 5. Déléguer au bon handler
Response response;
if (route.getCgiExtension() != null) {
    response = CGIHandler.handle(request, route, config);
} else if (request.getMethod().equals("POST")) {
    response = UploadHandler.handle(request, route, config);
} else if (request.getMethod().equals("DELETE")) {
    response = DeleteHandler.handle(request, route, config);
} else {
    response = StaticHandler.handle(request, route, config);
}

        // 6. Ajouter le cookie SID dans la réponse
        response.addHeader("Set-Cookie", Cookie.create("SID", session.getId()));

        return response;
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