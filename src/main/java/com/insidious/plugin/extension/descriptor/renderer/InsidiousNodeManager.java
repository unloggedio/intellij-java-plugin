package com.insidious.plugin.extension.descriptor.renderer;

import com.intellij.debugger.ui.tree.NodeDescriptor;
import com.insidious.plugin.extension.InsidiousDebuggerTreeNode;
import com.insidious.plugin.extension.evaluation.EvaluationContext;
import org.jetbrains.annotations.NotNull;

public interface InsidiousNodeManager {
  @NotNull
  InsidiousDebuggerTreeNode createMessageNode(String paramString);
  
  @NotNull
  InsidiousDebuggerTreeNode createNode(NodeDescriptor paramNodeDescriptor, EvaluationContext paramEvaluationContext);
}