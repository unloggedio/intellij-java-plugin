package com.insidious.plugin.extension.descriptor.renderer;

import com.insidious.plugin.extension.InsidiousChildRenderer;

public interface InsidiousNodeRenderer extends InsidiousChildRenderer, InsidiousValueLabelRenderer {
    String getName();

    void setName(String paramString);

    boolean isEnabled();

    void setEnabled(boolean paramBoolean);
}


