package com.insidious.plugin.extension.evaluation;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil;
import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup;
import com.sun.jdi.*;
import com.insidious.plugin.extension.DebuggerBundle;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousOnDemandRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public abstract class InsidiousNodeDescriptorImpl implements NodeDescriptor {
    public static final String UNKNOWN_VALUE_MESSAGE = "";
    protected static final Logger logger = LoggerUtil.getInstance(InsidiousNodeDescriptorImpl.class);
    private static final Key<Map<ObjectReference, ValueMarkup>> MARKUP_MAP_KEY = new Key("ValueMarkupMap");
    public boolean myIsExpanded = false;
    public boolean myIsSelected = false;
    public boolean myIsVisible = false;
    public boolean myIsSynthetic = false;
    private EvaluateException myEvaluateException;
    private String myLabel = "";
    private Map<Key, Object> myUserData;

    @Nullable
    public static Map<ObjectReference, ValueMarkup> getMarkupMap(XDebugProcess process) {
        if (process == null) {
            return null;
        }

        Map<ObjectReference, ValueMarkup> map = process.getProcessHandler().getUserData(MARKUP_MAP_KEY);
        if (map == null) {
            map = new HashMap<>();
            process.getProcessHandler().putUserData(MARKUP_MAP_KEY, map);
        }
        return map;
    }

    public String getName() {
        return null;
    }

    public <T> T getUserData(Key<T> key) {
        if (this.myUserData == null) {
            return null;
        }

        return (T) this.myUserData.get(key);
    }

    public <T> void putUserData(Key<T> key, T value) {
        if (this.myUserData == null) {
            this.myUserData = (Map<Key, Object>) new HashMap();
        }
        this.myUserData.put(key, value);
    }

    public void updateRepresentation(XDebugProcess process, DescriptorLabelListener labelListener) {
        updateRepresentationNoNotify(process, labelListener);
        labelListener.labelChanged();
    }

    public void updateRepresentationNoNotify(XDebugProcess process, DescriptorLabelListener labelListener) {
        try {
            try {
                this.myEvaluateException = null;
                this.myLabel = calcRepresentation(process, labelListener);
            } catch (InconsistentDebugInfoException e) {
                throw new EvaluateException(
                        DebuggerBundle.message("error.inconsistent.debug.info"));
            } catch (InvalidStackFrameException e) {
                throw new EvaluateException(DebuggerBundle.message("error.invalid.stackframe"));
            } catch (ObjectCollectedException e) {
                throw EvaluateExceptionUtil.OBJECT_WAS_COLLECTED;
            } catch (VMDisconnectedException e) {
                throw e;
            } catch (RuntimeException e) {
                if (e.getCause() instanceof InterruptedException) {
                    throw e;
                }
                throw new EvaluateException(DebuggerBundle.message("internal.debugger.error"));
            }
        } catch (EvaluateException e) {
            setFailed(e);
        }
    }

    protected abstract String calcRepresentation(XDebugProcess paramXDebugProcess, DescriptorLabelListener paramDescriptorLabelListener) throws EvaluateException;

    public void displayAs(NodeDescriptor descriptor) {
        if (descriptor instanceof InsidiousNodeDescriptorImpl) {
            InsidiousNodeDescriptorImpl that = (InsidiousNodeDescriptorImpl) descriptor;
            this.myIsExpanded = that.myIsExpanded;
            this.myIsSelected = that.myIsSelected;
            this.myIsVisible = that.myIsVisible;
            this.myUserData = (that.myUserData != null) ? new HashMap<>(that.myUserData) : null;


            if (this.myUserData != null) {
                this.myUserData.remove(InsidiousOnDemandRenderer.ON_DEMAND_CALCULATED);
            }
        }
    }

    public abstract boolean isExpandable();

    public abstract void setContext(EvaluationContext paramEvaluationContext);

    public EvaluateException getEvaluateException() {
        return this.myEvaluateException;
    }

    public String getLabel() {
        return this.myLabel;
    }

    public String toString() {
        return getLabel();
    }

    protected String setFailed(EvaluateException e) {
        this.myEvaluateException = e;
        return e.getMessage();
    }

    protected String setLabel(String customLabel) {
        return this.myLabel = customLabel;
    }

    public void clear() {
        this.myEvaluateException = null;
        this.myLabel = "";
    }

    public void setAncestor(NodeDescriptor oldDescriptor) {
        displayAs(oldDescriptor);
    }
}


