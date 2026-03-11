package com.javaserver.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.javaserver.routing.Route;

public class ServerConfig {

    private String host;
    private List<Integer> ports = new ArrayList<>();
    private Boolean isDefault = false;
    private String clientBodyLimit;
    private Map<String, String> errorPages = new HashMap<>();
    private List<Route> routes = new ArrayList<>();


    public ServerConfig fromMap(Map<?, ?> m) {
        if (m == null || m.isEmpty()) {
            System.err.println("[ERROR] Server config is empty or null");
            return null;
        }

        // host
        if (!m.containsKey("host")) {
            System.err.println("[ERROR] Server missing 'host'");
            return null;
        }
        this.host = (String) m.get("host");

        // default_server
        if (m.containsKey("default_server")) {
            Object def = m.get("default_server");
            if (!(def instanceof Boolean)) {
                System.err.println("[ERROR] 'default_server' must be boolean");
                return null;
            }
            this.isDefault = (Boolean) def;
        }

        // ports
        if (!m.containsKey("ports")) {
            System.err.println("[ERROR] Server '" + host + "' missing 'ports'");
            return null;
        }
        Object portsObj = m.get("ports");
        if (!(portsObj instanceof List)) {
            System.err.println("[ERROR] 'ports' must be a list");
            return null;
        }
        List<?> rawPorts = (List<?>) portsObj;
        if (rawPorts.isEmpty()) {
            System.err.println("[ERROR] 'ports' list is empty");
            return null;
        }
        for (Object p : rawPorts) {
            if (!(p instanceof Number)) {
                System.err.println("[ERROR] Each port must be a number");
                return null;
            }
            int port = ((Number) p).intValue();
            if (port < 1 || port > 65535) {
                System.err.println("[ERROR] Invalid port: " + port);
                return null;
            }
            this.ports.add(port);
        }

        // client_body_limit
        if (m.containsKey("client_body_limit")) {
            this.clientBodyLimit = (String) m.get("client_body_limit");
        }

        // error_pages
        if (m.containsKey("error_pages")) {
            Object ep = m.get("error_pages");
            if (ep instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) ep).entrySet()) {
                    errorPages.put(entry.getKey().toString(), entry.getValue().toString());
                }
            }
        }

        // routes
        if (!m.containsKey("routes")) {
            System.err.println("[ERROR] Server '" + host + "' missing 'routes'");
            return null;
        }
        Object routesObj = m.get("routes");
        if (!(routesObj instanceof List)) {
            System.err.println("[ERROR] 'routes' must be a list");
            return null;
        }
        for (Object r : (List<?>) routesObj) {
            if (!(r instanceof Map)) {
                System.err.println("[ERROR] Each route must be an object");
                return null;
            }
            Route route = new Route().fromMap((Map<?, ?>) r);
            if (route != null) this.routes.add(route);
        }

        return this;
    }

    // ─── Getters ───────────────────────────────────────────────────
    public String getHost() { return host; }
    public List<Integer> getPorts() { return ports; }
    public Boolean isDefault() { return isDefault; }
    public String getClientBodyLimit() { return clientBodyLimit; }
    public Map<String, String> getErrorPages() { return errorPages; }
    public List<Route> getRoutes() { return routes; }

    @Override
    public String toString() {
        return "ServerConfig{host='" + host + "', ports=" + ports +
               ", isDefault=" + isDefault + ", routes=" + routes.size() + "}";
    }
}