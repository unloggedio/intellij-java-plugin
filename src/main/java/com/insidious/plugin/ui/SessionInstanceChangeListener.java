package com.insidious.plugin.ui;

import com.insidious.plugin.client.UnloggedClientInterface;
import com.insidious.plugin.upload.SourceModel;
import com.intellij.openapi.project.Project;

public interface SessionInstanceChangeListener {

    UnloggedClientInterface setUnloggedClient(SourceModel sourceModel);

    UnloggedClientInterface getUnloggedClient();

    Project getProject();
}
