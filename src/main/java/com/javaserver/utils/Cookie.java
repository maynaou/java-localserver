
package com.javaserver.utils;

import java.util.HashMap;
import java.util.Map;

public class Cookie {

    private String name;
    private String value;
    private String path    = "/";
    private String domain  = null;
    private int maxAge     = -1;
    private boolean httpOnly = false;
    private boolean secure   = false;

    public Cookie(String name, String value) {
        this.name  = name;
        this.value = value;
    }

    // ─── Parse Cookie header → map ────────────────────────────────────────────
    // Cookie: sessionId=abc123; theme=dark  →  {sessionId=abc123, theme=dark}

    public static Map<String, String> parseCookieHeader(String cookieHeader) {
        Map<String, String> cookies = new HashMap<>();
        if (cookieHeader == null || cookieHeader.isEmpty()) return cookies;

        for (String pair : cookieHeader.split(";")) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String name  = pair.substring(0, eq).trim();
            String value = pair.substring(eq + 1).trim();
            cookies.put(name, value);
        }

        return cookies;
    }

    // ─── Build Set-Cookie header value ────────────────────────────────────────
    // sessionId=abc123; Path=/; Max-Age=3600; HttpOnly

    public String toSetCookieHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        if (path != null)   sb.append("; Path=").append(path);
        if (domain != null) sb.append("; Domain=").append(domain);
        if (maxAge >= 0)    sb.append("; Max-Age=").append(maxAge);
        if (httpOnly)       sb.append("; HttpOnly");
        if (secure)         sb.append("; Secure");
        return sb.toString();
    }

    // ─── Builder-style setters ────────────────────────────────────────────────

    public Cookie path(String path)     { this.path     = path;  return this; }
    public Cookie domain(String domain) { this.domain   = domain; return this; }
    public Cookie maxAge(int maxAge)    { this.maxAge   = maxAge; return this; }
    public Cookie httpOnly(boolean v)   { this.httpOnly = v;      return this; }
    public Cookie secure(boolean v)     { this.secure   = v;      return this; }

    // ─── Getters ──────────────────────────────────────────────────────────────

    public String getName()  { return name; }
    public String getValue() { return value; }
}