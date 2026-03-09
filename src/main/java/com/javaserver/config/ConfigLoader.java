package com.javaserver.config;

import java.util.List;

import com.javaserver.parser.ConfigParser;

public class ConfigLoader {

   public static List<ConfigServer> load(String filePath) {
      
     return ConfigParser.parse(filePath);
   }
}