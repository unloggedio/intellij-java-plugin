package com.insidious.plugin.extension;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.actions.SmartStepTarget;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XSourcePosition;
import com.intellij.xdebugger.frame.XSuspendContext;
import com.intellij.xdebugger.stepping.XSmartStepIntoHandler;
import com.intellij.xdebugger.stepping.XSmartStepIntoVariant;
import com.insidious.plugin.extension.model.DirectionType;
import com.insidious.plugin.extension.smartstep.InsidiousJavaSmartStepIntoHandlerImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import javax.swing.*;
import java.util.List;

public class InsidiousJavaSmartStepIntoActionHandler
        extends XSmartStepIntoHandler<InsidiousJavaSmartStepIntoActionHandler.JvmSmartStepIntoVariant> {
    private final InsidiousJavaDebugProcess myDebugProcess;
    private final InsidiousJavaSmartStepIntoHandler handler;

    public InsidiousJavaSmartStepIntoActionHandler(InsidiousJavaDebugProcess debugProcess) {
        this.myDebugProcess = debugProcess;
        this.handler = new InsidiousJavaSmartStepIntoHandlerImpl();
    }


    @NotNull
    public Promise<List<JvmSmartStepIntoVariant>> computeSmartStepVariantsAsync(@NotNull XSourcePosition position) {
        return findVariants(position, true);
    }


    @NotNull
    public Promise<List<JvmSmartStepIntoVariant>> computeStepIntoVariants(@NotNull XSourcePosition position) {
        return findVariants(position, false);
    }


    private Promise<List<JvmSmartStepIntoVariant>> findVariants(@NotNull XSourcePosition xPosition, boolean smart) {
        SourcePosition position = DebuggerUtilsEx.toSourcePosition(xPosition, this.myDebugProcess.getProject());
        if (this.handler.isAvailable(position)) {


            Promise<List<SmartStepTarget>> targets = smart ? this.handler.findSmartStepTargetsAsync(position, this.myDebugProcess) : this.handler.findStepIntoTargets(position, this.myDebugProcess);
//            return targets.then(results -> ContainerUtil.map(results, ()));
        }


        return Promises.rejectedPromise();
    }


    @NotNull
    public List<JvmSmartStepIntoVariant> computeSmartStepVariants(@NotNull XSourcePosition position) {
        throw new IllegalStateException("Should not be called");
    }


    public String getPopupTitle(@NotNull XSourcePosition position) {
        return DebuggerBundle.message("title.smart.step.popup");
    }


    public void stepIntoEmpty(XDebugSession session) {
        session.forceStepInto();
    }


    public void startStepInto(@NotNull JvmSmartStepIntoVariant variant, @Nullable XSuspendContext context) {
        this.myDebugProcess.stepInto(variant.myHandler.createMethodFilter(variant.myTarget), context);
        if (this.myDebugProcess.getLastDirectionType() == DirectionType.BACKWARDS) {
            this.myDebugProcess.addPerformanceAction(context, "reverse_smartstep_into");
        } else {
            this.myDebugProcess.addPerformanceAction(context, "smart_step_into");
        }
    }

    static class JvmSmartStepIntoVariant extends XSmartStepIntoVariant {
        private final SmartStepTarget myTarget;
        private final InsidiousJavaSmartStepIntoHandler myHandler;

        JvmSmartStepIntoVariant(SmartStepTarget target, InsidiousJavaSmartStepIntoHandler handler) {
            this.myTarget = target;
            this.myHandler = handler;
        }


        public String getText() {
            return this.myTarget.getPresentation();
        }


        @Nullable
        public Icon getIcon() {
            return this.myTarget.getIcon();
        }


        @Nullable
        public TextRange getHighlightRange() {
            PsiElement element = this.myTarget.getHighlightElement();
            return (element != null) ? element.getTextRange() : null;
        }
    }
}


