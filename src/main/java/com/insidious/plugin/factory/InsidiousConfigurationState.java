package com.insidious.plugin.factory;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "com.insidious.plugin.factory.InsidiousConfigurationState",
        storages = @Storage("InsidiousPlugin.xml")
)
public class InsidiousConfigurationState implements PersistentStateComponent<InsidiousConfigurationState> {

    public String username = "";
    public String serverUrl = "http://13.127.65.158";
    public Map<String, Boolean> exceptionClassMap;

    public InsidiousConfigurationState() {
        exceptionClassMap = new HashMap<>();
        exceptionClassMap.put("java.lang.NullPointerException", true);
        exceptionClassMap.put("java.lang.ArrayIndexOutOfBoundsException", true);
        exceptionClassMap.put("java.lang.StackOverflowError", true);
        exceptionClassMap.put("java.lang.IllegalArgumentException", true);
        exceptionClassMap.put("java.lang.IllegalThreadStateException", true);
        exceptionClassMap.put("java.lang.IllegalStateException", true);
        exceptionClassMap.put("java.lang.RuntimeException", true);
        exceptionClassMap.put("java.io.IOException", true);
        exceptionClassMap.put("java.io.FileNotFoundException", true);
        exceptionClassMap.put("java.net.SocketException", true);
        exceptionClassMap.put("java.net.UnknownHostException", true);
        exceptionClassMap.put("java.lang.ArithmeticException", true);
    }

    public static InsidiousConfigurationState getInstance(Project project) {
        return project.getService(InsidiousConfigurationState.class);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Nullable
    @Override
    public InsidiousConfigurationState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull InsidiousConfigurationState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}