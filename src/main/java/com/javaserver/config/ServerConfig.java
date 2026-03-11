package com.javaserver.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import routing.Route;

public class ServerConfig {

    private String host;
    private List<Integer> ports;
    private String root = "";
    private Map<String, String> errorPages = new HashMap<>();
    private Map<String, route> routes = new HashMap<>();
    private Boolean isdefault = false;

    public ServerConfig fromMap(Map<?, ?> m) {
        if (m == null || m.isEmpty()) {
            System.err.println("[ERROR] Server config is empty or null");
            return null;
        }
        if (m.containsKey("default")) {
            Object defaultObj = m.get("default");
            if (defaultObj instanceof Boolean) {
                this.isdefault = (Boolean) defaultObj;
            } else {
                System.err.println("[ERROR] default must be boolean");
                return null;
            }
        }
    }
}
