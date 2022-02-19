package extension.descriptor;

import com.intellij.debugger.SourcePosition;
import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.debugger.settings.ThreadsViewSettings;
import com.intellij.debugger.ui.impl.watch.MethodsTracker;
import com.intellij.debugger.ui.tree.render.DescriptorLabelListener;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.EmptyIcon;
import com.intellij.xdebugger.XDebugProcess;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.impl.XDebugSessionImpl;
import com.intellij.xdebugger.impl.frame.XValueMarkers;
import com.intellij.xdebugger.impl.ui.tree.ValueMarkup;
import com.sun.jdi.*;
import extension.InsidiousJavaDebugProcess;
import extension.thread.InsidiousStackFrameContext;
import extension.connector.InsidiousStackFrameProxy;
import extension.evaluation.EvaluationContext;
import extension.evaluation.InsidiousNodeDescriptorImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Collections;
import java.util.Map;

public class InsidiousStackFrameDescriptorImpl extends InsidiousNodeDescriptorImpl implements InsidiousStackFrameContext {
    private final InsidiousStackFrameProxy myFrame;
    private String myName = null;
    private int myUiIndex;
    private Location myLocation;
    private MethodsTracker.MethodOccurrence myMethodOccurrence;
    private boolean myIsSynthetic;
    private boolean myIsInLibraryContent;
    private ObjectReference myThisObject;
    private SourcePosition mySourcePosition;
    private XDebugProcess myProcess;
    private Project myProject;
    private Icon myIcon = AllIcons.Debugger.Frame;


    public InsidiousStackFrameDescriptorImpl(@NotNull InsidiousStackFrameProxy frame, @NotNull MethodsTracker tracker) {
        this.myFrame = frame;

        try {
            this.myUiIndex = frame.getFrameIndex();
            this.myLocation = frame.location();
            if (!getValueMarkers().isEmpty()) {
                getThisObject();
            }
            this
                    .myMethodOccurrence = tracker.getMethodOccurrence(this.myUiIndex, DebuggerUtilsEx.getMethod(this.myLocation));
            this.myIsSynthetic = DebuggerUtils.isSynthetic(this.myMethodOccurrence.getMethod());
            this.myProcess = frame.threadProxy().getVirtualMachine().getXDebugProcess();
            this.myProject = this.myProcess.getSession().getProject();
            if (this.myProcess instanceof InsidiousJavaDebugProcess) {
                InsidiousJavaDebugProcess InsidiousJavaDebugProcess = (extension.InsidiousJavaDebugProcess) this.myProcess;
                this.mySourcePosition = ReadAction.compute(() -> InsidiousJavaDebugProcess.getPositionManager().getSourcePosition(this.myLocation));
            }


            PsiFile psiFile = (this.mySourcePosition != null) ? this.mySourcePosition.getFile() : null;
            this
                    .myIsInLibraryContent = DebuggerUtilsEx.isInLibraryContent(
                    (psiFile != null) ? psiFile.getVirtualFile() : null, this.myProject);
        } catch (InternalException | EvaluateException e) {
            LOG.info(e);
            this.myLocation = null;
            this.myMethodOccurrence = tracker.getMethodOccurrence(0, null);
            this.myIsSynthetic = false;
            this.myIsInLibraryContent = false;
        }
    }

    public int getUiIndex() {
        return this.myUiIndex;
    }


    @NotNull
    public InsidiousStackFrameProxy getFrameProxy() {
        return this.myFrame;
    }


    @NotNull
    public XDebugProcess getXDebugProcess() {
        return this.myProcess;
    }

    @Nullable
    public Method getMethod() {
        return this.myMethodOccurrence.getMethod();
    }

    public int getOccurrenceIndex() {
        return this.myMethodOccurrence.getIndex();
    }

    public boolean isRecursiveCall() {
        return this.myMethodOccurrence.isRecursive();
    }

    @Nullable
    public ValueMarkup getValueMarkup() {
        Map<?, ValueMarkup> markers = getValueMarkers();
        if (!markers.isEmpty() && this.myThisObject != null) {
            return markers.get(this.myThisObject);
        }
        return null;
    }

    private Map<?, ValueMarkup> getValueMarkers() {
        XDebugProcess process = this.myFrame.threadProxy().getVirtualMachine().getXDebugProcess();
        XDebugSession session = process.getSession();
        if (session instanceof XDebugSessionImpl) {
            XValueMarkers<?, ?> markers = ((XDebugSessionImpl) session).getValueMarkers();
            if (markers != null) {
                return markers.getAllMarkers();
            }
        }
        return Collections.emptyMap();
    }


    public String getName() {
        return this.myName;
    }


    protected String calcRepresentation(XDebugProcess process, DescriptorLabelListener descriptorLabelListener) throws EvaluateException {
        this.myIcon = calcIcon();

        if (this.myLocation == null) {
            return "";
        }
        ThreadsViewSettings settings = ThreadsViewSettings.getInstance();
        StringBuilder label = new StringBuilder();
        Method method = this.myMethodOccurrence.getMethod();
        if (method != null) {
            this.myName = method.name();
            label.append(
                    settings.SHOW_ARGUMENTS_TYPES ?
                            DebuggerUtilsEx.methodNameWithArguments(method) :
                            this.myName);
        }
        if (settings.SHOW_LINE_NUMBER) {
            label.append(':').append(DebuggerUtilsEx.getLineNumber(this.myLocation, false));
        }
        if (settings.SHOW_CLASS_NAME) {
            String name;
            try {
                ReferenceType refType = this.myLocation.declaringType();
                name = (refType != null) ? refType.name() : null;
            } catch (InternalError e) {
                name = e.toString();
            }
            if (name != null) {
                label.append(", ");
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex < 0) {
                    label.append(name);
                } else {
                    label.append(name.substring(dotIndex + 1));
                    if (settings.SHOW_PACKAGE_NAME) {
                        label.append(" {");
                        label.append(name, 0, dotIndex);
                        label.append("}");
                    }
                }
            }
        }
        if (settings.SHOW_SOURCE_NAME) {
            label.append(", ")
                    .append(DebuggerUtilsEx.getSourceName(this.myLocation, e -> "Unknown Source"));
        }
        return label.toString();
    }


    public boolean isExpandable() {
        return true;
    }


    public final void setContext(EvaluationContext context) {
    }

    public boolean isSynthetic() {
        return this.myIsSynthetic;
    }

    public boolean isInLibraryContent() {
        return this.myIsInLibraryContent;
    }

    @Nullable
    public Location getLocation() {
        return this.myLocation;
    }

    public SourcePosition getSourcePosition() {
        return this.mySourcePosition;
    }

    private Icon calcIcon() {
        if (this.myFrame == null) {
            return AllIcons.Debugger.Db_obsolete;
        }
        return EmptyIcon.create(6);
    }

    public Icon getIcon() {
        return this.myIcon;
    }

    @Nullable
    public ObjectReference getThisObject() {
        if (this.myThisObject == null) {
            try {
                this.myThisObject = this.myFrame.thisObject();
            } catch (EvaluateException e) {
                LOG.info(e);
            }
        }
        return this.myThisObject;
    }
}

