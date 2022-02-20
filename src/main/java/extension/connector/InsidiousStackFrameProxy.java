package extension.connector;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import com.intellij.debugger.engine.jdi.StackFrameProxy;
import com.sun.jdi.*;
import extension.thread.InsidiousLocalVariableProxy;
import extension.thread.InsidiousThreadReferenceProxy;

import java.util.List;

public interface InsidiousStackFrameProxy extends StackFrameProxy {

    InsidiousThreadReferenceProxy threadProxy();

    String getVariableName(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy) throws EvaluateException;

    Value getValue(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy) throws EvaluateException;

    void setValue(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy, Value paramValue) throws ClassNotLoadedException, InvalidTypeException, EvaluateException;

    Type getType(InsidiousLocalVariableProxy paramInsidiousLocalVariableProxy) throws EvaluateException, ClassNotLoadedException;

    InsidiousLocalVariableProxy visibleVariableByName(String paramString) throws EvaluateException;

    List<InsidiousLocalVariableProxy> visibleVariables();

    List<Value> getArgumentValues() throws EvaluateException;

    ObjectReference thisObject() throws EvaluateException;
}
