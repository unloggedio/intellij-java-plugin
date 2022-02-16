package extension.descriptor.renderer;

import extension.InsidiousChildRenderer;

public interface InsidiousNodeRenderer extends InsidiousChildRenderer, InsidiousValueLabelRenderer {
    String getName();

    void setName(String paramString);

    boolean isEnabled();

    void setEnabled(boolean paramBoolean);
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\render\InsidiousNodeRenderer.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */