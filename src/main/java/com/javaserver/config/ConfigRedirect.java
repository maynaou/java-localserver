package com.javaserver.config;

public class ConfigRedirect {
    private final int code;
    private final String target;

    public ConfigRedirect(int code, String target) {
        this.code = code;
        this.target = target;
    }

    public int getCode() {
        return code;
    }

    public String getTarget() {
        return target;
    }

}
