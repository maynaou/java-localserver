package com.javaserver.routing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Route {

    private String path;
    private List<String> methods = new ArrayList<>();
    private String root;
    private String defaultFile;
    private Boolean directoryListing = false;
    private String defaultDirectoryFile;
    private String clientBodyLimit;
    private Map<String, String> cgi = new HashMap<>();
    private Redirect redirect;

    // ─── fromMap ───────────────────────────────────────────────────
    public Route fromMap(Map<?, ?> m) {
        if (m == null || m.isEmpty()) {
            System.err.println("[ERROR] Route is empty or null");
            return null;
        }

        // path (required)
        if (!m.containsKey("path")) {
            System.err.println("[ERROR] Route missing 'path'");
            return null;
        }
        this.path = (String) m.get("path");

        // methods (required)
        if (!m.containsKey("methods")) {
            System.err.println("[ERROR] Route '" + path + "' missing 'methods'");
            return null;
        }
        Object methodsObj = m.get("methods");
        if (!(methodsObj instanceof List)) {
            System.err.println("[ERROR] Route '" + path + "' 'methods' must be a list");
            return null;
        }
        List<String> allowed = List.of("GET", "POST", "DELETE");
        for (Object method : (List<?>) methodsObj) {
            String m2 = method.toString().toUpperCase();
            if (!allowed.contains(m2)) {
                System.err.println("[ERROR] Route '" + path + "' invalid method: " + m2);
                return null;
            }
            this.methods.add(m2);
        }
        if (this.methods.isEmpty()) {
            System.err.println("[ERROR] Route '" + path + "' has no valid methods");
            return null;
        }

        // redirect (optional) — if present, root is not required
        if (m.containsKey("redirect")) {
            Object redirectObj = m.get("redirect");
            if (!(redirectObj instanceof Map)) {
                System.err.println("[ERROR] Route '" + path + "' 'redirect' must be an object");
                return null;
            }
            this.redirect = new Redirect().fromMap((Map<?, ?>) redirectObj);
            if (this.redirect == null) return null;
            return this; // redirect routes don't need root
        }

        // root (required if no redirect)
        if (!m.containsKey("root")) {
            System.err.println("[ERROR] Route '" + path + "' missing 'root'");
            return null;
        }
        this.root = (String) m.get("root");

        // default_file (optional)
        if (m.containsKey("default_file")) {
            this.defaultFile = (String) m.get("default_file");
        }

        // directory_listing (optional)
        if (m.containsKey("directory_listing")) {
            Object dl = m.get("directory_listing");
            if (!(dl instanceof Boolean)) {
                System.err.println("[ERROR] Route '" + path + "' 'directory_listing' must be boolean");
                return null;
            }
            this.directoryListing = (Boolean) dl;
        }

        // default_directory_file (optional)
        if (m.containsKey("default_directory_file")) {
            this.defaultDirectoryFile = (String) m.get("default_directory_file");
        }

        // client_body_limit (optional)
        if (m.containsKey("client_body_limit")) {
            this.clientBodyLimit = (String) m.get("client_body_limit");
        }

        // cgi (optional)
        if (m.containsKey("cgi")) {
            Object cgiObj = m.get("cgi");
            if (!(cgiObj instanceof Map)) {
                System.err.println("[ERROR] Route '" + path + "' 'cgi' must be an object");
                return null;
            }
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) cgiObj).entrySet()) {
                this.cgi.put(entry.getKey().toString(), entry.getValue().toString());
            }
        }

        return this;
    }

    // ─── Getters ───────────────────────────────────────────────────
    public String getPath() { return path; }
    public List<String> getMethods() { return methods; }
    public String getRoot() { return root; }
    public String getDefaultFile() { return defaultFile; }
    public Boolean getDirectoryListing() { return directoryListing; }
    public String getDefaultDirectoryFile() { return defaultDirectoryFile; }
    public String getClientBodyLimit() { return clientBodyLimit; }
    public Map<String, String> getCgi() { return cgi; }
    public Redirect getRedirect() { return redirect; }

    // ─── Helpers ───────────────────────────────────────────────────
    public boolean hasMethod(String method) {
        return methods.contains(method.toUpperCase());
    }

    public boolean isCgiRoute() {
        return !cgi.isEmpty();
    }

    public boolean isRedirectRoute() {
        return redirect != null;
    }

    public String getCgiExecutor(String extension) {
        return cgi.get(extension); // e.g. cgi.get(".py") → "python3"
    }

    @Override
    public String toString() {
        return "Route{path='" + path + "', methods=" + methods +
               ", root='" + root + "', redirect=" + redirect +
               ", cgi=" + cgi + "}";
    }
}