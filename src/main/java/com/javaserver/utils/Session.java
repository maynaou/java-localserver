package com.javaserver.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Session {

    private final String id;
    private final Map<String, String> data;

    public Session() {
        this.id   = UUID.randomUUID().toString();
        this.data = new HashMap<>();
    }

    public String getId()  { return id; }

    public void set(String key, String value) { data.put(key, value); }
    public String get(String key) { return data.get(key); }
    public boolean has(String key) { return data.containsKey(key); }
}
