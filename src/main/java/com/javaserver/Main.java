package com.javaserver;

import java.util.List;

import com.javaserver.config.ConfigLoader;
import com.javaserver.config.ServerConfig;
import com.javaserver.server.Server;

public class Main {

    public static void main(String[] args) {
        String cfgPath = (args.length > 0) ? args[0] : "config.json";
        System.out.println(cfgPath + " ffffffffffff");
        List<ServerConfig> Listconfigs = ConfigLoader.load(cfgPath);
        Server server = new Server();
        server.start();
    }
}
