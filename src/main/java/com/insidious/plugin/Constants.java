package com.insidious.plugin;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Constants {
        
    private static String getHostname() {
        String defaultHostname = System.getProperty("user.name");

        if (defaultHostname.length() <= 4){
                defaultHostname = "short-" + defaultHostname;
        }

        return defaultHostname;
    }

    public static final Path HOME_PATH = FileSystems.getDefault()
            .getPath(System.getProperty("user.home"), ".unlogged");
    public static final String COMPLETED = "completed";
    public static final String PENDING = "pending";
    public static final String AGENT_VERSION = "0.4.19";
    public static final String HOSTNAME = getHostname();
}
