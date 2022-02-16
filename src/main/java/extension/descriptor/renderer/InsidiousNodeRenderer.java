package extension.descriptor.renderer;

import extension.InsidiousChildRenderer;

public interface InsidiousNodeRenderer extends InsidiousChildRenderer, InsidiousValueLabelRenderer {
    String getName();

    void setName(String paramString);

    boolean isEnabled();

    void setEnabled(boolean paramBoolean);
}


