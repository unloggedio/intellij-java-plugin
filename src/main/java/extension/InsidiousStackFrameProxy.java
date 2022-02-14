package extension;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.LocalVariableProxy;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.intellij.debugger.engine.jdi.ThreadReferenceProxy;
import com.intellij.debugger.engine.jdi.VirtualMachineProxy;
import com.sun.jdi.*;

import java.util.List;

public interface InsidiousStackFrameProxy extends StackFrameProxy {

    InsidiousThreadReferenceProxy threadProxy();

    String getVariableName(InsidiousLocalVariableProxy paramUndoLocalVariableProxy) throws EvaluateException;

    Value getValue(InsidiousLocalVariableProxy paramUndoLocalVariableProxy) throws EvaluateException;

    void setValue(InsidiousLocalVariableProxy paramUndoLocalVariableProxy, Value paramValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException;

    Type getType(InsidiousLocalVariableProxy paramUndoLocalVariableProxy) throws EvaluateException, ClassNotLoadedException;

    InsidiousLocalVariableProxy visibleVariableByName(String paramString) throws EvaluateException;

    List<InsidiousLocalVariableProxy> visibleVariables();

    List<Value> getArgumentValues() throws EvaluateException;

    ObjectReference thisObject() throws EvaluateException;
}
