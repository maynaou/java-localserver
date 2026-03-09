package com.javaserver.parser;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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

        System.out.println(json);
        JsonParser parser = new JsonParser(json);
        System.out.println(parser.getSrc());
        System.out.println(parser.getPos());
        Map<String, Object> root = parser.parse();
        System.out.println(root);
        return null;
    }
}
