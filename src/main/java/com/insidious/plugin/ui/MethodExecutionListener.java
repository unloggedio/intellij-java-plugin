package com.insidious.plugin.ui;

import com.insidious.plugin.pojo.atomic.StoredCandidate;
import com.insidious.plugin.ui.methodscope.AgentCommandResponseListener;
import com.intellij.psi.PsiClass;

import java.awt.*;
import java.util.List;

public interface MethodExecutionListener {

    void executeCandidate(
            List<StoredCandidate> metadata,
            PsiClass psiClass,
            String source,
            AgentCommandResponseListener<String> stringAgentCommandResponseListener
    );

    void displayResponse(Component responseComponent);
}
