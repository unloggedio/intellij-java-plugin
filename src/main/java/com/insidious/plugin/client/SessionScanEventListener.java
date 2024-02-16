package com.insidious.plugin.client;

public interface SessionScanEventListener {
    void started();
    void waiting();
    void paused();
    void ended();
    void progress(ScanProgress scanProgress);
}
