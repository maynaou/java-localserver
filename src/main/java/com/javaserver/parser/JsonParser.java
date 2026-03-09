package com.javaserver.parser;

import java.util.Map;

public class JsonParser {
    private final String src; // le JSON complet
    private int pos;          // curseur : où on en est dans la lecture

    public JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public Map<String, Object> parse() {
        skipWhitespace();
        return parseValue();
    }

    public Map<String,Object> parseValue() {
        skipWhitespace();
        char c = peek();
        System.out.println(c);
        return null;
    }

    private void skipWhitespace() {
        System.out.println(src.charAt(pos));
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            System.out.println("----");
            pos++;
        };
    }

    private char peek() {
        skipWhitespace();
        return pos < src.length() ? src.charAt(pos) : '\0';
    }

    public String getSrc() {
        return src;
    }

    public int getPos() {
        return pos;
    }
}
