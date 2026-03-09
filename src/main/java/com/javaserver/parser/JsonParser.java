package com.javaserver.parser;

import java.util.Map;

public class JsonParser {
    private final String src; // le JSON complet
    private final int pos;          // curseur : où on en est dans la lecture

    public JsonParser(String src) {
        this.src = src;
        this.pos = 0;
    }

    public Map<String, Object> parse() {
        return null;
    }

    public String getSrc() {
        return src;
    }

    public int getPos() {
        return pos;
    }
}
