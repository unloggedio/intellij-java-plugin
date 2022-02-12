package extension;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebugSessionListener;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.breakpoints.XBreakpoint;
import com.intellij.xdebugger.breakpoints.XLineBreakpoint;
import com.intellij.xdebugger.frame.XExecutionStack;
import com.intellij.xdebugger.frame.XStackFrame;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.stepping.XSmartStepIntoVariant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;

public class InsidiousDebuggerSession implements XDebugSession {
    @Override
    public @NotNull Project getProject() {
        return null;
    }

    @Override
    public @NotNull XDebugProcess getDebugProcess() {
        return null;
    }

    @Override
    public boolean isSuspended() {
        return false;
    }

    @Override
    public @Nullable XStackFrame getCurrentStackFrame() {
        return null;
    }

    @Override
    public XSuspendContext getSuspendContext() {
        return null;
    }

    @Override
    public @Nullable XSourcePosition getCurrentPosition() {
        return null;
    }

    @Override
    public @Nullable XSourcePosition getTopFramePosition() {
        return null;
    }

    @Override
    public void stepOver(boolean ignoreBreakpoints) {
        new Exception().printStackTrace();
    }

    @Override
    public void stepInto() {
        new Exception().printStackTrace();

    }

    @Override
    public void stepOut() {
        new Exception().printStackTrace();

    }

    @Override
    public void forceStepInto() {
        new Exception().printStackTrace();

    }

    @Override
    public void runToPosition(@NotNull XSourcePosition position, boolean ignoreBreakpoints) {
        new Exception().printStackTrace();

    }

    @Override
    public void pause() {
        new Exception().printStackTrace();

    }

    @Override
    public void resume() {
        new Exception().printStackTrace();

    }

    @Override
    public void showExecutionPoint() {
        new Exception().printStackTrace();

    }

    @Override
    public void setCurrentStackFrame(@NotNull XExecutionStack executionStack, @NotNull XStackFrame frame, boolean isTopFrame) {
        new Exception().printStackTrace();

    }

    @Override
    public void updateBreakpointPresentation(@NotNull XLineBreakpoint<?> breakpoint, @Nullable Icon icon, @Nullable String errorMessage) {
        new Exception().printStackTrace();

    }

    @Override
    public void setBreakpointVerified(@NotNull XLineBreakpoint<?> breakpoint) {
        new Exception().printStackTrace();

    }

    @Override
    public void setBreakpointInvalid(@NotNull XLineBreakpoint<?> breakpoint, @Nullable String errorMessage) {
        new Exception().printStackTrace();

    }

    @Override
    public boolean breakpointReached(@NotNull XBreakpoint<?> breakpoint, @Nullable String evaluatedLogExpression, @NotNull XSuspendContext suspendContext) {
        return false;
    }

    @Override
    public boolean breakpointReached(@NotNull XBreakpoint<?> breakpoint, @NotNull XSuspendContext suspendContext) {
        return false;
    }

    @Override
    public void positionReached(@NotNull XSuspendContext suspendContext) {
        new Exception().printStackTrace();

    }

    @Override
    public void sessionResumed() {
        new Exception().printStackTrace();

    }

    @Override
    public void stop() {
        new Exception().printStackTrace();

    }

    @Override
    public void setBreakpointMuted(boolean muted) {
        new Exception().printStackTrace();

    }

    @Override
    public boolean areBreakpointsMuted() {
        return false;
    }

    @Override
    public void addSessionListener(@NotNull XDebugSessionListener listener, @NotNull Disposable parentDisposable) {
        new Exception().printStackTrace();

    }

    @Override
    public void addSessionListener(@NotNull XDebugSessionListener listener) {
        new Exception().printStackTrace();

    }

    @Override
    public void removeSessionListener(@NotNull XDebugSessionListener listener) {
        new Exception().printStackTrace();

    }

    @Override
    public void reportError(@NotNull @NlsContexts.NotificationContent String message) {
        new Exception().printStackTrace();

    }

    @Override
    public void reportMessage(@NotNull @NlsContexts.NotificationContent String message, @NotNull MessageType type) {
        new Exception().printStackTrace();

    }

    @Override
    public void reportMessage(@NotNull @NlsContexts.NotificationContent String message, @NotNull MessageType type, @Nullable HyperlinkListener listener) {
        new Exception().printStackTrace();

    }

    @Override
    public @NotNull @NlsContexts.TabTitle String getSessionName() {
        return null;
    }

    @Override
    public @NotNull RunContentDescriptor getRunContentDescriptor() {
        return null;
    }

    @Override
    public @Nullable RunProfile getRunProfile() {
        return null;
    }

    @Override
    public void setPauseActionSupported(boolean isSupported) {
        new Exception().printStackTrace();

    }

    @Override
    public void rebuildViews() {
        new Exception().printStackTrace();

    }

    @Override
    public <V extends XSmartStepIntoVariant> void smartStepInto(XSmartStepIntoHandler<V> handler, V variant) {
        new Exception().printStackTrace();

    }

    @Override
    public void updateExecutionPosition() {
        new Exception().printStackTrace();

    }

    @Override
    public void initBreakpoints() {
        new Exception().printStackTrace();

    }

    @Override
    public ConsoleView getConsoleView() {
        return null;
    }

    @Override
    public RunnerLayoutUi getUI() {
        return null;
    }

    @Override
    public boolean isStopped() {
        return false;
    }

    @Override
    public boolean isPaused() {
        return false;
    }
}
