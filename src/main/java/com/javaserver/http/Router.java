package com.javaserver.http;

import com.javaserver.config.ServerConfig;
import com.javaserver.errors.ErrorHandler;
import com.javaserver.http.HttpRequest;
import com.javaserver.http.HttpResponse;
import com.javaserver.routing.Route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Router {

    private final ServerConfig config;

    public Router(ServerConfig config) {
        this.config = config;
    }

    public HttpResponse handle(HttpRequest req) {
        // Find matching route
        Route route = matchRoute(req.getPath());
        if (route == null) {
            return com.javaserver.errors.ErrorHandler.handle(404, config.getErrorPages());
        }

        // Check redirect first
        if (route.isRedirectRoute()) {
            int code = route.getRedirect().getCode(); // 301 or 302
            String location = route.getRedirect().getUrl();
            return HttpResponse.redirect(code, location);
        }

        // Check method is allowed
        List<String> allowed = route.getMethods();
        if (allowed != null && !allowed.isEmpty()
                && !allowed.contains(req.getMethod())) {
            return ErrorHandler.handle(405, config.getErrorPages());
        }

        // Dispatch by method
        try {
            switch (req.getMethod()) {
                case "GET":    return handleGet(req, route);
                case "POST":   return handlePost(req, route);
                case "DELETE": return handleDelete(req, route);
                default:       return ErrorHandler.handle(405, config.getErrorPages());
            }
        } catch (Exception e) {
            System.err.println("Router error: " + e.getMessage());
            return ErrorHandler.handle(500, config.getErrorPages());
        }
    }

    // ─── GET ──────────────────────────────────────────────────────────────────

    private HttpResponse handleGet(HttpRequest req, Route route) throws IOException {
        Path root = Paths.get(route.getRoot());
        Path target = root.resolve(req.getPath().substring(1)).normalize();

        // Block path traversal: resolved path must stay inside root
        if (!target.startsWith(root)) {
            return ErrorHandler.handle(403, config.getErrorPages());
        }

        // Directory request
        if (Files.isDirectory(target)) {
            // Try default file first (e.g. index.html)
            if (route.getDefaultFile() != null) {
                Path defaultFile = target.resolve(route.getDefaultFile());
                if (Files.exists(defaultFile)) {
                    return serveFile(defaultFile);
                }
            }

            // Directory listing
            if (Boolean.TRUE.equals(route.getDirectoryListing())) {
                return buildDirectoryListing(target, req.getPath());
            }

            return ErrorHandler.handle(403, config.getErrorPages());
        }

        // File request
        if (!Files.exists(target)) {
            return ErrorHandler.handle(404, config.getErrorPages());
        }

        return serveFile(target);
    }

    private HttpResponse serveFile(Path file) throws IOException {
        byte[] content = Files.readAllBytes(file);
        String contentType = Files.probeContentType(file.toFile().toPath());
        if (contentType == null) contentType = "application/octet-stream";
        return HttpResponse.ok().setBody(content, contentType);
    }

    private HttpResponse buildDirectoryListing(Path dir, String requestPath) throws IOException {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><body>");
        html.append("<h2>Index of ").append(requestPath).append("</h2><ul>");

        List<Path> entries = Files.list(dir).sorted().collect(Collectors.toList());
        for (Path entry : entries) {
            String name = entry.getFileName().toString();
            if (Files.isDirectory(entry)) name += "/";
            html.append("<li><a href=\"").append(name).append("\">")
                .append(name).append("</a></li>");
        }

        html.append("</ul></body></html>");
        return HttpResponse.ok().setBody(html.toString(), "text/html");
    }

    // ─── POST ─────────────────────────────────────────────────────────────────

    private HttpResponse handlePost(HttpRequest req, Route route) throws IOException {
        String uploadDir = route.getRoot();
        if (uploadDir == null) {
            return ErrorHandler.handle(500, config.getErrorPages());
        }

        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // Derive filename from path e.g. POST /upload/file.txt → file.txt
        String filename = Paths.get(req.getPath()).getFileName().toString();
        if (filename.isEmpty()) {
            filename = "upload_" + System.currentTimeMillis();
        }

        Path target = dir.resolve(filename).normalize();

        // Block path traversal
        if (!target.startsWith(dir)) {
            return ErrorHandler.handle(403, config.getErrorPages());
        }

        Files.write(target, req.getBody());
        return HttpResponse.created()
                .setBody("File uploaded: " + filename, "text/plain");
    }

    // ─── DELETE ───────────────────────────────────────────────────────────────

    private HttpResponse handleDelete(HttpRequest req, Route route) throws IOException {
        Path root = Paths.get(route.getRoot());
        Path target = root.resolve(req.getPath().substring(1)).normalize();

        // Block path traversal
        if (!target.startsWith(root)) {
            return ErrorHandler.handle(403, config.getErrorPages());
        }

        if (!Files.exists(target)) {
            return ErrorHandler.handle(404, config.getErrorPages());
        }

        if (Files.isDirectory(target)) {
            return ErrorHandler.handle(403, config.getErrorPages());
        }

        Files.delete(target);
        return HttpResponse.noContent();
    }

    // ─── Route matching ───────────────────────────────────────────────────────

    private Route matchRoute(String path) {
        Route best = null;
        int bestLen = -1;

        for (Route route : config.getRoutes()) {
            String routePath = route.getPath();
            if (path.startsWith(routePath) && routePath.length() > bestLen) {
                best    = route;
                bestLen = routePath.length();
            }
        }

        return best;
    }
}