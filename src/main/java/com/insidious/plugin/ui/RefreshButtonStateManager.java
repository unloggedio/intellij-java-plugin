package com.insidious.plugin.ui;

import java.util.Date;

public interface RefreshButtonStateManager {
    void setState_NewLogs(Date lastScannedTimeStamp);
    void setState_NewSession();
    void setState_NoNewLogs(Date lastScannedTimeStamp);
    void setState_Processing();
    boolean isProcessing();
}
