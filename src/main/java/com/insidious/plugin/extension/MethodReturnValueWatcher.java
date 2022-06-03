package com.insidious.plugin.extension;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.debugger.ui.overhead.OverheadProducer;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.SimpleColoredComponent;
import com.sun.jdi.*;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.event.MethodExitEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;
import com.sun.jdi.request.MethodExitRequest;
import com.insidious.plugin.extension.util.DebuggerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MethodReturnValueWatcher implements OverheadProducer {
    private static final Logger logger = LoggerUtil.getInstance("#com.intellij.debugger.engine.requests.MethodReturnValueWatcher");
    private final EventRequestManager myRequestManager;
    @Nullable
    private Method myLastExecutedMethod;
    @Nullable
    private Value myLastMethodReturnValue;
    private ThreadReference myThread;
    @Nullable
    private MethodEntryRequest myEntryRequest;
    @Nullable
    private Method myEntryMethod;
    @Nullable
    private MethodExitRequest myExitRequest;
    private volatile boolean myTrackingEnabled;

    public MethodReturnValueWatcher(EventRequestManager requestManager) {
        this.myRequestManager = requestManager;
    }

    private void processMethodExitEvent(MethodExitEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("<- " + event.method());
        }
        try {
//            if (Registry.is("debugger.watch.return.speedup") && this.myEntryMethod != null) {
//                if (this.myEntryMethod.equals(event.method())) {
//                    LOG.debug("Now watching all");
//                    enableEntryWatching(true);
//                    this.myEntryMethod = null;
//                    createExitRequest().enable();
//                } else {
//                    return;
//                }
//            }
            Method method = event.method();
            Value retVal = event.returnValue();

            if (method == null || !DebuggerUtil.isVoid(method)) {

                this.myLastExecutedMethod = method;
                this.myLastMethodReturnValue = retVal;
            }
        } catch (UnsupportedOperationException ex) {
            logger.error("failed", ex);
        }
    }

    private void processMethodEntryEvent(MethodEntryEvent event) {
        if (logger.isDebugEnabled()) {
            logger.debug("-> " + event.method());
        }
        try {
            if (this.myEntryRequest != null && this.myEntryRequest.isEnabled()) {
                this.myExitRequest = createExitRequest();
                this.myExitRequest.addClassFilter(event.method().declaringType());
                this.myEntryMethod = event.method();
                this.myExitRequest.enable();

                if (logger.isDebugEnabled()) {
                    logger.debug("Now watching only " + event.method());
                }

                enableEntryWatching(false);
            }
        } catch (VMDisconnectedException e) {
            throw e;
        } catch (Exception e) {
            logger.error("failed", e);
        }
    }

    private void enableEntryWatching(boolean enable) {
        if (this.myEntryRequest != null) {
            this.myEntryRequest.setEnabled(enable);
        }
    }

    @Nullable
    public Method getLastExecutedMethod() {
        return this.myLastExecutedMethod;
    }

    @Nullable
    public Value getLastMethodReturnValue() {
        return this.myLastMethodReturnValue;
    }


    public boolean isEnabled() {
        return (DebuggerSettings.getInstance()).WATCH_RETURN_VALUES;
    }


    public void setEnabled(boolean enabled) {
        (DebuggerSettings.getInstance()).WATCH_RETURN_VALUES = enabled;
        clear();
    }

    public boolean isTrackingEnabled() {
        return this.myTrackingEnabled;
    }

    public void enable(ThreadReference thread) {
        setTrackingEnabled(true, thread);
    }

    public void disable() {
        setTrackingEnabled(false, null);
    }

    private void setTrackingEnabled(boolean trackingEnabled, ThreadReference thread) {
        this.myTrackingEnabled = trackingEnabled;
        updateRequestState((trackingEnabled && isEnabled()), thread);
    }

    public void clear() {
        this.myLastExecutedMethod = null;
        this.myLastMethodReturnValue = null;
        this.myThread = null;
    }

    private void updateRequestState(boolean enabled, @Nullable ThreadReference thread) {
        try {
            if (this.myEntryRequest != null) {
                this.myRequestManager.deleteEventRequest(this.myEntryRequest);
                this.myEntryRequest = null;
            }
            if (this.myExitRequest != null) {
                this.myRequestManager.deleteEventRequest(this.myExitRequest);
                this.myExitRequest = null;
            }
            if (enabled) {
                clear();
                this.myThread = thread;

//                if (Registry.is("debugger.watch.return.speedup")) {
//                    createEntryRequest().enable();
//                }
                createExitRequest().enable();
            }
        } catch (ObjectCollectedException objectCollectedException) {
        }
    }


    private MethodEntryRequest createEntryRequest() {
        this.myEntryRequest = prepareRequest(this.myRequestManager.createMethodEntryRequest());
        return this.myEntryRequest;
    }

    @NotNull
    private MethodExitRequest createExitRequest() {
        if (this.myExitRequest != null) {
            this.myRequestManager.deleteEventRequest(this.myExitRequest);
        }
        this.myExitRequest = prepareRequest(this.myRequestManager.createMethodExitRequest());
        return this.myExitRequest;
    }

    @NotNull
    private <T extends EventRequest> T prepareRequest(T request) {
//        request.setSuspendPolicy(
//                Registry.is("debugger.watch.return.speedup") ?
//                        1 :
//                        0);
        if (this.myThread != null) {
            if (request instanceof MethodEntryRequest) {
                ((MethodEntryRequest) request).addThreadFilter(this.myThread);
            } else if (request instanceof MethodExitRequest) {
                ((MethodExitRequest) request).addThreadFilter(this.myThread);
            }
        }
        request.putProperty("WATCHER_REQUEST_KEY", Boolean.valueOf(true));
        return request;
    }

    public boolean processEvent(Event event) {
        EventRequest request = event.request();
        if (request == null || request.getProperty("WATCHER_REQUEST_KEY") == null) {
            return false;
        }

        if (event instanceof MethodEntryEvent) {
            processMethodEntryEvent((MethodEntryEvent) event);
        } else if (event instanceof MethodExitEvent) {
            processMethodExitEvent((MethodExitEvent) event);
        }
        return true;
    }


    public void customizeRenderer(SimpleColoredComponent renderer) {
        renderer.setIcon(AllIcons.Debugger.WatchLastReturnValue);
        renderer.append(DebuggerBundle.message("action.watches.method.return.value.enable"));
    }
}


