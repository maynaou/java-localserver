package com.javaserver.routing;


import java.util.List;
import java.util.Map;

public class Route {
    private String path;
    private List<String> methods;
    private String root;

    @JsonProperty("default_file")
    private String defaultFile;

    @JsonProperty("directory_listing")
    private Boolean directoryListing;

    @JsonProperty("default_directory_file")
    private String defaultDirectoryFile;

    @JsonProperty("client_body_limit")
    private String clientBodyLimit;

    private Map<String, String> cgi;
    private Redirect redirect;

    public String getPath() { return path; }
    public List<String> getMethods() { return methods; }
    public String getRoot() { return root; }
    public String getDefaultFile() { return defaultFile; }
    public Boolean getDirectoryListing() { return directoryListing; }
    public String getDefaultDirectoryFile() { return defaultDirectoryFile; }
    public String getClientBodyLimit() { return clientBodyLimit; }
    public Map<String, String> getCgi() { return cgi; }
    public Redirect getRedirect() { return redirect; }
}