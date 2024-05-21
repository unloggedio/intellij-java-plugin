package com.insidious.plugin.ui;

import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.upload.SourceModel;

public interface SessionInstanceChangeListener {

    VideobugClientInterface modifySessionInstance(SourceModel sourceModel);
}
