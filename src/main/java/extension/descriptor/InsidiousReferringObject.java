package extension.descriptor;

import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.frame.XValueNode;
import com.sun.jdi.Value;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface InsidiousReferringObject {
  @NotNull
  InsidiousValueDescriptorImpl createValueDescription(@NotNull Project paramProject, @NotNull Value paramValue);
  
  @Nullable
  String getNodeName(int paramInt);
  
  @NotNull
  Function<XValueNode, XValueNode> getNodeCustomizer();
}


/* Location:              D:\workspace\code\LR4J-IntelliJ-Plugin-6.6.1\lr4j-IntelliJ\li\\Insidious-intellij-6.6.1\!\i\\Insidious\intellij\debugger\descriptor\InsidiousReferringObject.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       1.1.3
 */