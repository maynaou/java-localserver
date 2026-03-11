package com.javaserver.config;

import java.util.List;

public class ConfigRoute {
    private final String path;
    private final List<String> methods;
    private final String root;
    private final String defaultFile;
    private final boolean directoryListing;
    private final String defaultDirectoryFile;
    private final List<ConfigRedirect> redirects;
    private final String cgiExtension;

    public ConfigRoute(String path, List<String> methods, String root, String defaultFile, boolean directoryListing, String defaultDirectoryFile, List<ConfigRedirect> redirects, String cgiExtension) {
        this.path = path;
        this.methods = methods;
        this.root = root;
        this.defaultFile = defaultFile;
        this.directoryListing = directoryListing;
        this.defaultDirectoryFile = defaultDirectoryFile;
        this.redirects = redirects;
        this.cgiExtension = cgiExtension;
    }

    public String getPath() {
        return path;
    }

    public List<String> getMethods() {
        return methods;
    }

    public String getRoot() {
        return root;
    }

    public String getDefaultFile() {
        return defaultFile;
    }

    public boolean isDirectoryListing() {
        return directoryListing;
    }

    public String getDefaultDirectoryFile() {
        return defaultDirectoryFile;
    }

    public List<ConfigRedirect> getRedirects() {
        return redirects;
    }

    public String getCgiExtension() {
        return cgiExtension;
    }

    @Override
public String toString() {
    return "\n  [Route] path=" + path
         + " | methods=" + methods
         + " | root=" + root
         + " | dirListing=" + directoryListing;
}
}
