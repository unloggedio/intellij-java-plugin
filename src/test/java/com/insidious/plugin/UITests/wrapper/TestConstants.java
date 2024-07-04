package com.insidious.plugin.UITests.wrapper;

import static com.insidious.plugin.Constants.AGENT_VERSION;

public class TestConstants {
    public static final String REMOTE_URL = "http://localhost:8123";
    public static final String DEFAULT_PRE_POPULATED_URL = "http://unlogged.local:8123";
    public static String MAVEN_DEPENDENCY_TEMPLATE = "<dependency>\n" +
            "  <artifactId>unlogged-sdk</artifactId>\n" +
            "  <groupId>video.bug</groupId>\n" +
            "  <version>" + AGENT_VERSION + "</version>\n" +
            "</dependency>";

    public static String GRADLE_DEPENDENCY_TEMPLATE = "dependencies\n" +
            "{\n" +
            "    implementation 'video.bug:unlogged-sdk:" + AGENT_VERSION + "'\n" +
            "    annotationProcessor 'video.bug:unlogged-sdk:" + AGENT_VERSION + "'\n" +
            "}";
}
