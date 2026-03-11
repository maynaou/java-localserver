package com.javaserver;

import java.util.List;
import java.util.Map;

import com.javaserver.config.Config;
import com.javaserver.config.ConfigLoader;
import com.javaserver.config.ServerConfig;
import com.javaserver.routing.Route;
import com.javaserver.server.Server;

public class Main {

    public static void main(String[] args) {
        String cfgPath = (args.length > 0) ? args[0] : "config.json";
        List<ServerConfig> listConfigs = ConfigLoader.load(cfgPath);

        for (ServerConfig server : listConfigs) {
            System.out.println("=== Server ===");
            System.out.println("  Host        : " + server.getHost());
            System.out.println("  Ports       : " + server.getPorts());
            System.out.println("  Default     : " + server.isDefault());
            System.out.println("  Body Limit  : " + server.getClientBodyLimit());

            System.out.println("  Error Pages :");
            for (Map.Entry<String, String> entry : server.getErrorPages().entrySet()) {
                System.out.println("    " + entry.getKey() + " -> " + entry.getValue());
            }

            System.out.println("  Routes      :");
            for (Route route : server.getRoutes()) {
                System.out.println("    --- Route ---");
                System.out.println("      Path      : " + route.getPath());
                System.out.println("      Methods   : " + route.getMethods());
                System.out.println("      Root      : " + route.getRoot());

                if (route.getDefaultFile() != null)
                    System.out.println("      DefFile   : " + route.getDefaultFile());

                if (route.getDirectoryListing() != null)
                    System.out.println("      DirList   : " + route.getDirectoryListing());

                if (route.getClientBodyLimit() != null)
                    System.out.println("      BodyLimit : " + route.getClientBodyLimit());

                if (route.isCgiRoute())
                    System.out.println("      CGI       : " + route.getCgi());

                if (route.isRedirectRoute())
                    System.out.println("      Redirect  : " + route.getRedirect());
            }
            System.out.println();
        }

        Server server = new Server();
        server.start();
    }
}
