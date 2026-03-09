package com.javaserver;

import java.util.List;

import com.javaserver.config.ConfigLoader;
import com.javaserver.config.ConfigServer;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        // System.out.println( "Hello World!" );
        List<ConfigServer> servers = ConfigLoader.load("config.json");
        System.out.println(servers);
    }
}
