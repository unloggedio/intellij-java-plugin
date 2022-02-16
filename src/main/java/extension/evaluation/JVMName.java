package extension.evaluation;

import com.intellij.debugger.engine.evaluation.EvaluateException;
import extension.InsidiousVirtualMachineProxy;

public interface JVMName {
    String getName(InsidiousVirtualMachineProxy paramInsidiousVirtualMachineProxy) throws EvaluateException;

    String getDisplayName(InsidiousVirtualMachineProxy paramInsidiousVirtualMachineProxy);
}
