package com.insidious.plugin.factory;

import com.amplitude.Amplitude;
import com.amplitude.Event;
import org.json.JSONObject;

import java.util.Locale;

import static com.insidious.plugin.factory.InsidiousService.HOSTNAME;

public class UsageInsightTracker {
    private final Amplitude amplitudeClient;

    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_VERSION = System.getProperty("os.version");
    private static final String LANGUAGE = Locale.getDefault().getLanguage();
    private static final String JAVA_VERSION = Locale.getDefault().getLanguage();
    private static final String OS_TAG = OS_NAME + ":" + OS_VERSION;
    private static UsageInsightTracker instance;
    private static final Object lock = new Object();
    private final VersionManager versionManager;

    public static UsageInsightTracker getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (lock) {
            if (instance != null) {
                return instance;
            }
            instance = new UsageInsightTracker();
            return instance;
        }
    }

    private UsageInsightTracker() {
        amplitudeClient = Amplitude.getInstance("PLUGIN");
        amplitudeClient.init("993c17091c853700ea386f71df5fb72c");
        versionManager = new VersionManager();
    }

    public void RecordEvent(String eventName, JSONObject eventProperties) {
        Event event = new Event(eventName, HOSTNAME);
        event.osName = OS_TAG;
        event.language = LANGUAGE;
        event.appVersion = versionManager.getVersion();
        event.eventProperties = eventProperties;
        amplitudeClient.logEvent(event);
    }
}
