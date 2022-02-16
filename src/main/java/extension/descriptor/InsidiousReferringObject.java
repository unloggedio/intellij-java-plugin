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

