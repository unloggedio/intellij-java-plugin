package com.insidious.plugin.factory;

import com.amplitude.Amplitude;
import com.amplitude.Event;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.PermanentInstallationID;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.insidious.plugin.Constants.HOSTNAME;

public class UsageInsightTracker {
    private static final String OS_NAME = System.getProperty("os.name");
    private static final String OS_VERSION = System.getProperty("os.version");
    private static final String LANGUAGE = Locale.getDefault().getLanguage();
    private static final String OS_TAG = OS_NAME + ":" + OS_VERSION;
    public static final String REMOTE_IP_AMPLITUDE_CONST = "$remote";
    private static UsageInsightTracker instance;
    private final Amplitude amplitudeClient;
    private final VersionManager versionManager;
    private final List<String> UsersToSkip = Arrays.asList(
            "artpar",
            "Amogh",
            "testerfresher",
            "short-kt"
    );
    private final long sessionId = new Date().getTime();
    private final AtomicInteger eventId = new AtomicInteger();
    private boolean shutdown = false;

    private UsageInsightTracker() {
        amplitudeClient = Amplitude.getInstance("PLUGIN");
        amplitudeClient.init("993c17091c853700ea386f71df5fb72c");
        versionManager = new VersionManager();
    }

    public static UsageInsightTracker getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (UsageInsightTracker.class) {
            if (instance != null) {
                return instance;
            }
            instance = new UsageInsightTracker();
        }
        return instance;
    }

    public void RecordEvent(String eventName, JSONObject eventProperties) {
        if (shutdown || UsersToSkip.contains(HOSTNAME)) {
            return;
        }
        Event event = new Event(eventName, HOSTNAME);
        event.platform = TimeZone.getDefault().getID();
//        event.country = TimeZone.getDefault().getID();
        event.osName = OS_TAG;
//        event.language = LANGUAGE;
        event.ip = REMOTE_IP_AMPLITUDE_CONST;
        event.sessionId = sessionId;
        event.appVersion = versionManager.getVersion();
        event.deviceId = PermanentInstallationID.get();
        event.eventId = eventId.getAndIncrement();
        event.deviceModel = ApplicationInfo.getInstance().getFullVersion();
        event.eventProperties = eventProperties;
        amplitudeClient.logEvent(event);
    }

    public void close() {
        shutdown = true;
        try {
            amplitudeClient.shutdown();
        } catch (InterruptedException e) {
            // throw new RuntimeException(e);
        }
    }
}
