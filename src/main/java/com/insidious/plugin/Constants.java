package com.insidious.plugin;

import okhttp3.MediaType;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class Constants {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final String TOKEN = "token";
    public static final String PROJECT_URL = "/api/data/projects";

    public static final Path VIDEOBUG_HOME_PATH = FileSystems.getDefault().getPath(System.getProperty("user.home"), ".videobug");
    public static final String AGENT_JAR_NAME = "videobug-java-agent.jar";
    public static final Path VIDEOBUG_AGENT_PATH = FileSystems.getDefault().getPath(VIDEOBUG_HOME_PATH.toAbsolutePath().toString(), AGENT_JAR_NAME);
    public static final Path VIDEOBUG_SESSIONS_PATH = FileSystems.getDefault().getPath(VIDEOBUG_HOME_PATH.toAbsolutePath().toString(),"sessions");
    public static final String COMPLETED = "completed";
    public static final String PENDING = "pending";
}
