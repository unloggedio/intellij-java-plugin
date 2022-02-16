package extension;

import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.openapi.project.Project;
import extension.descriptor.renderer.InsidiousNodeRenderer;

public interface InsidiousDebuggerTreeNode {
  InsidiousDebuggerTreeNode getParent();
  
  NodeDescriptor getDescriptor();
  
  Project getProject();
  
  void setRenderer(InsidiousNodeRenderer paramInsidiousNodeRenderer);
}
