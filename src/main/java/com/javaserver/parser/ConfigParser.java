package com.javaserver.parser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import com.javaserver.config.ConfigServer;

public class ConfigParser {
    public static List<ConfigServer> parse(String filePath) {
        String json;
        try {
            json = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (Exception e) {
            throw new RuntimeException("[ConfigParser] Impossible de lire: " + filePath, e);
        }

        JsonParser parser = new JsonParser(json);
        Map<String, Object> root = parser.parse();
        System.out.println(root);
        return null;
    }
}
