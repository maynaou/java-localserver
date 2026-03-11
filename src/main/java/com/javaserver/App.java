package com.javaserver;

import java.util.List;

import com.javaserver.config.ConfigLoader;
import com.javaserver.config.ConfigServer;
import com.javaserver.server.Server;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        List<ConfigServer> servers = ConfigLoader.load("config.json");
        System.out.println(servers);

        for (ConfigServer server : servers) {
            System.out.println(server);
        }

        try {
            Server s = new Server(servers);
            s.start();
        } catch (Exception e) {
            e.printStackTrace();
             System.out.println("Erreur lors du démarrage du serveur: " + e.getMessage());
        }

        
    }
}
