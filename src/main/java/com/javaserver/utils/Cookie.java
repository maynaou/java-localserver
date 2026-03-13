package com.javaserver.utils;

import java.util.HashMap;
import java.util.Map;

public class Cookie {

    // Parser les cookies depuis le header
    // ex: "SID=abc123; theme=dark" → {"SID": "abc123", "theme": "dark"}
    public static Map<String, String> parse(String cookieHeader) {
        Map<String, String> cookies = new HashMap<>();

        if (cookieHeader == null || cookieHeader.isEmpty()) {
            return cookies;
        }

        String[] parts = cookieHeader.split(";");
        for (String part : parts) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2) {
                cookies.put(kv[0].trim(), kv[1].trim());
            }
        }
        return cookies;
    }

    // Créer un cookie header
    // ex: "SID=abc123; Path=/; HttpOnly"
    public static String create(String name, String value) {
        return name + "=" + value + "; Path=/; HttpOnly";
    }

    // Récupérer un cookie par nom
    public static String get(String cookieHeader, String name) {
        Map<String, String> cookies = parse(cookieHeader);
        return cookies.get(name);
    }
}