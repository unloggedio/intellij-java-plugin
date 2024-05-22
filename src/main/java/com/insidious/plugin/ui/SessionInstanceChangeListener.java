package com.insidious.plugin.ui;

import com.insidious.plugin.client.UnloggedClientInterface;
import com.insidious.plugin.upload.SourceModel;

public interface SessionInstanceChangeListener {

    UnloggedClientInterface setUnloggedClient(SourceModel sourceModel);
}
