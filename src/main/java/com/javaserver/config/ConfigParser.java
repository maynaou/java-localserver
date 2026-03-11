package com.javaserver.config;

import java.io.File;
import java.util.Map;

public class ConfigParser {

    private static final long MB = 1024 * 1024;

    public static Map<String, Object> parse(String filePath) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new ConfigParseException("Empty input", 0);
        }

        validateFileExists(filePath);



        return null;
    }

    private static void validateFileExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new RuntimeException("Config file not found: " + filePath);
        }
        if (!file.isFile()) {
            throw new RuntimeException("Config path is not a file: " + filePath);
        }
        if (!file.canRead()) {
            throw new RuntimeException("Config file is not readable: " + filePath);
        }
    }

    public static class ConfigParseException extends RuntimeException {

        public ConfigParseException(String message, int position) {
            super(message + " at position " + position);
        }

        public ConfigParseException(String message, int position, Throwable cause) {
            super(message + " at position " + position, cause);
        }
    }
}
