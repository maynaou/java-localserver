package com.javaserver.errors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.javaserver.http.HttpResponse;

public class ErrorHandler {

    // Call this anywhere you need an error response.
    // It checks the server config for a custom error page first,
    // then falls back to a built-in HTML page.
    public static HttpResponse handle(int statusCode, Map<String, String> errorPages) {
        String message = getMessage(statusCode);

        // Try custom error page from config
        if (errorPages != null) {
            String pagePath = errorPages.get(String.valueOf(statusCode));
            if (pagePath != null) {
                try {
                    byte[] content = Files.readAllBytes(Paths.get(pagePath));
                    return new HttpResponse(statusCode, message)
                            .setBody(content, "text/html");
                } catch (IOException e) {
                    // Custom page missing — fall through to default
                    System.err.println("Custom error page not found: " + pagePath);
                }
            }
        }

        // Default built-in error page
        String html = "<!DOCTYPE html><html><body>"
                + "<h1>" + statusCode + " " + message + "</h1>"
                + "</body></html>";

        return new HttpResponse(statusCode, message)
                .setBody(html, "text/html");
    }

    // Overload: no custom pages
    public static HttpResponse handle(int statusCode) {
        return handle(statusCode, null);
    }

    private static String getMessage(int code) {
        switch (code) {
            case 400: return "Bad Request";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 413: return "Request Entity Too Large";
            case 500: return "Internal Server Error";
            default:  return "Error";
        }
    }
}