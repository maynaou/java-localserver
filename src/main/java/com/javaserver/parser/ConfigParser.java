package com.javaserver.parser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

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
        return null;
    }
}
