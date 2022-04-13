package com.insidious.plugin.extension.descriptor.renderer;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.ui.tree.render.BasicRendererProperties;
import com.intellij.debugger.ui.tree.render.Renderer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import com.sun.jdi.Type;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public abstract class InsidiousTypeRenderer
        implements Renderer {
    private static final Logger logger = LoggerUtil.getInstance(InsidiousTypeRenderer.class);
    protected BasicRendererProperties myProperties = new BasicRendererProperties(false);

    protected InsidiousTypeRenderer() {
        this("java.lang.Object");
    }

    protected InsidiousTypeRenderer(@NotNull String className) {
        this.myProperties.setClassName(className);
    }

    public String getClassName() {
        return this.myProperties.getClassName();
    }

    public void setClassName(String className) {
        this.myProperties.setClassName(className);
    }


    public Renderer clone() {
        try {
            InsidiousTypeRenderer cloned = (InsidiousTypeRenderer) super.clone();
            cloned.myProperties = this.myProperties.clone();
            return cloned;
        } catch (CloneNotSupportedException e) {
            logger.error("failed", e);

            return null;
        }
    }

    public boolean isApplicable(Type type) {
        return DebuggerUtils.instanceOf(type, getClassName());
    }


    public void writeExternal(Element element) throws WriteExternalException {
        this.myProperties.writeExternal(element, "java.lang.Object");
    }


    public void readExternal(Element element) throws InvalidDataException {
        this.myProperties.readExternal(element, "java.lang.Object");
    }

    protected InsidiousCachedEvaluator createCachedEvaluator() {
        return new InsidiousCachedEvaluator() {
            protected String getClassName() {
                return InsidiousTypeRenderer.this.getClassName();
            }
        };
    }
}


