package extension;

public interface TelemetryTracker {
    void actionStart(String paramString);

    void actionComplete();

    void endSession();

    String getSessionId();

    void setLicenceId(String paramString);

    void setBridgeBuildId(String paramString);

    boolean isShareUsageStatistics();

    void addRecording(String paramString1, String paramString2, String paramString3, long paramLong, String paramString4);
}


