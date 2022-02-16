package extension.descriptor;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.xdebugger.frame.*;
import com.sun.jdi.ObjectCollectedException;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.Value;
import extension.DebuggerBundle;
import extension.descriptor.renderer.InsidiousNodeManagerImpl;
import extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

public class InsidiousJavaReferringObjectsValue extends InsidiousJavaValue {
    private static final long MAX_REFERRING = 100L;
    private final InsidiousReferringObjectsProvider myReferringObjectsProvider;
    private final Function<XValueNode, XValueNode> myNodeConfigurator;

    private InsidiousJavaReferringObjectsValue(@Nullable InsidiousJavaValue parent,
                                               @NotNull InsidiousValueDescriptorImpl valueDescriptor,
                                               @NotNull EvaluationContext evaluationContext,
                                               @NotNull InsidiousReferringObjectsProvider referringObjectsProvider,
                                               InsidiousNodeManagerImpl nodeManager,
                                               @Nullable Function<XValueNode, XValueNode> nodeConfigurator) {
        super(parent, valueDescriptor, evaluationContext, nodeManager, false);
        this.myReferringObjectsProvider = referringObjectsProvider;
        this.myNodeConfigurator = nodeConfigurator;
    }


    public InsidiousJavaReferringObjectsValue(@NotNull InsidiousJavaValue javaValue, @NotNull InsidiousReferringObjectsProvider referringObjectsProvider, @Nullable Function<XValueNode, XValueNode> nodeConfigurator) {
        super(null, javaValue

                .getName(), javaValue
                .getDescriptor(), javaValue
                .getEvaluationContext(), javaValue
                .getNodeManager(), false);

        this.myReferringObjectsProvider = referringObjectsProvider;
        this.myNodeConfigurator = nodeConfigurator;
    }


    @Nullable
    public XReferrersProvider getReferrersProvider() {
        return new XReferrersProvider() {
            public XValue getReferringObjectsValue() {
                return new InsidiousJavaReferringObjectsValue(InsidiousJavaReferringObjectsValue.this, InsidiousJavaReferringObjectsValue.this
                        .myReferringObjectsProvider, null);
            }
        };
    }

    public void computeChildren(@NotNull XCompositeNode node) {
        List<InsidiousReferringObject> referringObjects;
        XValueChildrenList children = new XValueChildrenList();

        Value value = getDescriptor().getValue();


        try {
            referringObjects = this.myReferringObjectsProvider.getReferringObjects(
                    getEvaluationContext(), (ObjectReference) value, 100L);
        } catch (ObjectCollectedException e) {
            node.setErrorMessage(DebuggerBundle.message("evaluation.error.object.collected", new Object[0]));
            return;
        } catch (EvaluateException e) {
            node.setErrorMessage(e.getMessage());

            return;
        }
        int i = 1;
        for (InsidiousReferringObject object : referringObjects) {
            String nodeName = object.getNodeName(i++);
            InsidiousValueDescriptorImpl descriptor = object.createValueDescription(getProject(), value);


            InsidiousJavaReferringObjectsValue referringValue = new InsidiousJavaReferringObjectsValue(null, descriptor, getEvaluationContext(), this.myReferringObjectsProvider, getNodeManager(), object.getNodeCustomizer());
            if (nodeName == null) {
                children.add(referringValue);
                continue;
            }
            children.add(nodeName, referringValue);
        }


        node.addChildren(children, true);
    }


    public void computePresentation(@NotNull XValueNode node, @NotNull XValuePlace place) {
        super.computePresentation(
                (this.myNodeConfigurator == null) ? node : this.myNodeConfigurator.apply(node), place);
    }


    @Nullable
    public XValueModifier getModifier() {
        return null;
    }
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousJavaReferringObjectsValue.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */