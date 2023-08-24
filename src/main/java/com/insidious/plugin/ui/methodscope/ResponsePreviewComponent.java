package com.insidious.plugin.ui.methodscope;

import com.insidious.plugin.pojo.atomic.StoredCandidate;

import java.awt.*;

public interface ResponsePreviewComponent {
    Component get();
    void setTestCandidate(StoredCandidate candidate);
}
