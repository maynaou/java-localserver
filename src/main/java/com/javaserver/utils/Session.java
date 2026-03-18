package com.javaserver.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Session {

    private static final Map<String, Map<String, String>> store = new ConcurrentHashMap<>();

    private static final String COOKIE_NAME = "sessionId";
    private static final int    MAX_AGE     = 3600; // 1 hour

    // ─── Get existing session or create new one ───────────────────────────────

    public static String getOrCreate(Map<String, String> cookies) {
        String sessionId = cookies.get(COOKIE_NAME);

        if (sessionId != null && store.containsKey(sessionId)) {
            return sessionId; // existing valid session
        }

        // New session
        sessionId = UUID.randomUUID().toString();
        store.put(sessionId, new HashMap<>());
        return sessionId;
    }

    // ─── Read / write session data ────────────────────────────────────────────

    public static String get(String sessionId, String key) {
        Map<String, String> data = store.get(sessionId);
        return data != null ? data.get(key) : null;
    }

    public static void set(String sessionId, String key, String value) {
        Map<String, String> data = store.get(sessionId);
        if (data != null) data.put(key, value);
    }

    public static void remove(String sessionId, String key) {
        Map<String, String> data = store.get(sessionId);
        if (data != null) data.remove(key);
    }

    public static void destroy(String sessionId) {
        store.remove(sessionId);
    }

    // ─── Build Set-Cookie header for new session ──────────────────────────────

    public static String buildCookieHeader(String sessionId) {
        return new Cookie(COOKIE_NAME, sessionId)
                .path("/")
                .httpOnly(true)
                .maxAge(MAX_AGE)
                .toSetCookieHeader();
    }

    public static String getCookieName() { return COOKIE_NAME; }
}