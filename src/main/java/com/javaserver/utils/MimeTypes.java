package com.javaserver.utils;

public class MimeTypes {

    public static String getMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";

        // Text
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".htm"))  return "text/html";
        if (fileName.endsWith(".css"))  return "text/css";
        if (fileName.endsWith(".js"))   return "application/javascript";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".xml"))  return "application/xml";
        if (fileName.endsWith(".txt"))  return "text/plain";
        if (fileName.endsWith(".csv"))  return "text/csv";

        // Images
        if (fileName.endsWith(".png"))  return "image/png";
        if (fileName.endsWith(".jpg"))  return "image/jpeg";
        if (fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".gif"))  return "image/gif";
        if (fileName.endsWith(".svg"))  return "image/svg+xml";
        if (fileName.endsWith(".ico"))  return "image/x-icon";
        if (fileName.endsWith(".webp")) return "image/webp";

        // Audio/Video
        if (fileName.endsWith(".mp4"))  return "video/mp4";
        if (fileName.endsWith(".mp3"))  return "audio/mpeg";
        if (fileName.endsWith(".wav"))  return "audio/wav";

        // Documents
        if (fileName.endsWith(".pdf"))  return "application/pdf";
        if (fileName.endsWith(".zip"))  return "application/zip";

        // Par défaut
        return "application/octet-stream";
    }
}