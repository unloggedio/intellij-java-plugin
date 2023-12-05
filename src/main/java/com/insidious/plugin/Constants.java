package com.insidious.plugin;

import okhttp3.MediaType;

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

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String TOKEN = "token";
    public static final String PROJECT_URL = "/api/data/projects";

    public static final Path HOME_PATH = FileSystems.getDefault()
            .getPath(System.getProperty("user.home"), ".unlogged");
    public static final String AGENT_JAR_NAME = "unlogged-java-agent.jar";
    public static final String AGENT_INFO_NAME = "agent.txt";
    public static final Path AGENT_PATH = FileSystems.getDefault()
            .getPath(HOME_PATH.toAbsolutePath().toString(), AGENT_JAR_NAME);
    public static final Path AGENT_INFO_PATH = FileSystems.getDefault()
            .getPath(HOME_PATH.toAbsolutePath().toString(), AGENT_INFO_NAME);
    public static final Path SESSIONS_PATH = FileSystems.getDefault()
            .getPath(HOME_PATH.toAbsolutePath().toString(),"sessions");
    public static final String COMPLETED = "completed";
    public static final String PENDING = "pending";
    public static final String AGENT_VERSION = "1.14.3";
    public static final String HOSTNAME = getHostname();
}
