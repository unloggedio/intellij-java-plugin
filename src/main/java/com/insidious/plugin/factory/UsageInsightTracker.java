package com.insidious.plugin.factory;

import com.amplitude.Amplitude;
import com.amplitude.Event;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static com.insidious.plugin.factory.InsidiousService.HOSTNAME;

public class UsageInsightTracker {
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_VERSION = System.getProperty("os.version");
    private static final String LANGUAGE = Locale.getDefault().getLanguage();
    private static final String JAVA_VERSION = Locale.getDefault().getLanguage();
    private static final String OS_TAG = OS_NAME + ":" + OS_VERSION;
    private static final Object lock = new Object();
    private static UsageInsightTracker instance;
    private final Amplitude amplitudeClient;
    private final VersionManager versionManager;
    private final List<String> UsersToSkip = Arrays.asList(
            "artpar",
            "testerfresher"
    );

    private UsageInsightTracker() {
        amplitudeClient = Amplitude.getInstance("PLUGIN");
        amplitudeClient.init("993c17091c853700ea386f71df5fb72c");
        versionManager = new VersionManager();
    }

    public static UsageInsightTracker getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (lock) {
            if (instance != null) {
                return instance;
            }
            instance = new UsageInsightTracker();
        }
        return instance;
    }

    public void RecordEvent(String eventName, JSONObject eventProperties) {
        if (UsersToSkip.contains(HOSTNAME)) {
            return;
        }
        Event event = new Event(eventName, HOSTNAME);
        event.platform = OS_TAG;
        event.country = TimeZone.getDefault().getID();
        event.osName = OS_TAG;
        event.language = LANGUAGE;
        event.appVersion = versionManager.getVersion();
        event.eventProperties = eventProperties;
        amplitudeClient.logEvent(event);
    }
}
