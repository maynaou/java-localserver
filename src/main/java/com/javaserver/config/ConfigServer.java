package com.javaserver.config;

import java.util.List;
import java.util.Map;

public class ConfigServer {
    private final String host;
    private final List<Integer> ports;
    private final boolean defaultServer;
    private final String clientBodyLimit;
    private final Map<String, String> errorPages;
    private final List<ConfigRoute> routes;

    public ConfigServer(String host, List<Integer> ports, boolean defaultServer, 
                       String clientBodyLimit, Map<String, String> errorPages, 
                       List<ConfigRoute> routes) {
        this.host = host;
        this.ports = ports;
        this.defaultServer = defaultServer;
        this.clientBodyLimit = clientBodyLimit;
        this.errorPages = errorPages;
        this.routes = routes;
    }

    public String getHost() {
        return host;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public boolean isDefaultServer() {
        return defaultServer;
    }

    public String getClientBodyLimit() {
        return clientBodyLimit;
    }

    public Map<String, String> getErrorPages() {
        return errorPages;
    }

    public List<ConfigRoute> getRoutes() {
        return routes;
    }


    @Override
public String toString() {
    return "\n[Server] host=" + host
         + " | ports=" + ports
         + " | default=" + defaultServer
         + " | bodyLimit=" + clientBodyLimit
         + " | errorPages=" + errorPages.keySet()
         + " | routes=" + routes;
}
}
