package com.insidious.plugin.extension;

import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.insidious.plugin.extension.descriptor.renderer.InsidiousNodeRenderer;

public interface InsidiousDebuggerTreeNode {
  InsidiousDebuggerTreeNode getParent();
  
  NodeDescriptor getDescriptor();
  
  Project getProject();
  
  void setRenderer(InsidiousNodeRenderer paramInsidiousNodeRenderer);
}
