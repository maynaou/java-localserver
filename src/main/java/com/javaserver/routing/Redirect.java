package com.javaserver.routing;

import java.util.Map;

public class Redirect {

    private String url;
    private int code;

    // ─── fromMap ───────────────────────────────────────────────────
    public Redirect fromMap(Map<?, ?> m) {
        if (m == null || m.isEmpty()) {
            System.err.println("[ERROR] Redirect is empty or null");
            return null;
        }

        // url (required)
        if (!m.containsKey("url")) {
            System.err.println("[ERROR] Redirect missing 'url'");
            return null;
        }
        this.url = (String) m.get("url");

        // code (required)
        if (!m.containsKey("code")) {
            System.err.println("[ERROR] Redirect missing 'code'");
            return null;
        }
        Object codeObj = m.get("code");
        if (!(codeObj instanceof Number)) {
            System.err.println("[ERROR] Redirect 'code' must be a number");
            return null;
        }
        this.code = ((Number) codeObj).intValue();
        if (code != 301 && code != 302) {
            System.err.println("[ERROR] Redirect 'code' must be 301 or 302");
            return null;
        }

        return this;
    }

    // ─── Getters ───────────────────────────────────────────────────
    public String getUrl() { return url; }
    public int getCode() { return code; }

    @Override
    public String toString() {
        return "Redirect{url='" + url + "', code=" + code + "}";
    }
}