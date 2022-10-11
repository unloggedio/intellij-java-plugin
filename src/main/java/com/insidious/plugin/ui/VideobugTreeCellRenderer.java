package com.insidious.plugin.ui;

import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousRunConfigTypeInterface;
import com.insidious.plugin.factory.InsidiousService;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.util.Calendar;

import static com.intellij.util.IconUtil.createImageIcon;

public class VideobugTreeCellRenderer extends DefaultTreeCellRenderer {
    private static final String SPAN_FORMAT = "<span style='color:%s;'>%s</span>";
    private final DefaultTreeCellRenderer fallback;
    private final Icon offlineIcon;
    private final Icon methodIcon;
    private final Icon classIcon;
    private final Icon packageIcon;
    private final Icon sessionIcon;
    private InsidiousService insidiousService;
    private Icon onlineIcon;

    public VideobugTreeCellRenderer(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        this.fallback = new DefaultTreeCellRenderer();

        this.onlineIcon = IconLoader.getIcon("icons/png/outline-screen-disabled.png", VideobugTreeCellRenderer.class);
        this.offlineIcon = IconLoader.getIcon("icons/png/outline-screen.png", VideobugTreeCellRenderer.class);
        this.methodIcon = IconLoader.getIcon("icons/png/icons8-letter-m-14.png", VideobugTreeCellRenderer.class);
        this.sessionIcon = IconLoader.getIcon("icons/png/icons8-folder-14.png",
                VideobugTreeCellRenderer.class);
        this.packageIcon = IconLoader.getIcon("icons/png/icons8-p-14.png",
                VideobugTreeCellRenderer.class);
        this.classIcon = IconLoader.getIcon("icons/png/icons8-c-14.png",
                VideobugTreeCellRenderer.class);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object userObject, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
        super.getTreeCellRendererComponent(tree, userObject, sel, expanded, leaf, row, hasFocus);
        if (userObject instanceof ProbeInfoModel) {
            ProbeInfoModel probeInfoModel = (ProbeInfoModel) userObject;
//            String text = String.format(SPAN_FORMAT, "#d6caf0", probeInfoModel);
            String text = probeInfoModel.toString();
//            text += " [" + String.format(SPAN_FORMAT, "orange", pp.getRole()) + "]";
            this.setText("<html>" + text + "</html>");
//            this.setIcon(employeeIcon);
        } else if (userObject instanceof TreeClassInfoModel) {
            TreeClassInfoModel classInfoModel = (TreeClassInfoModel) userObject;
//            String text = String.format(SPAN_FORMAT, "#aba497", classInfoModel);
            String text = classInfoModel.toString();
            this.setText("<html>" + text + "</html>");
            this.setIcon(classIcon);
        } else if (userObject instanceof ExecutionSession) {
            ExecutionSession executionSession = (ExecutionSession) userObject;
//            String text = String.format(SPAN_FORMAT, "#aba497", executionSession);
            String text = executionSession.toString();
            this.setIcon(this.sessionIcon);
            this.setText("<html>" + text + "</html>");
        } else if (userObject instanceof MethodInfoModel) {
            MethodInfoModel methodInfoModel  = (MethodInfoModel) userObject;
//            String text = String.format(SPAN_FORMAT, "#719775", methodInfoModel);
            String text = methodInfoModel.toString();
            this.setText("<html>" + text + "</html>");
            this.setIcon(methodIcon);
        } else if (userObject instanceof PackageInfoModel) {
            PackageInfoModel packageInfoModel  = (PackageInfoModel) userObject;
            String text = String.format(SPAN_FORMAT, "#719775", packageInfoModel);
            this.setText("<html>" + text + "</html>");
            this.setIcon(packageIcon);
        } else {
            String text = String.format("%s", userObject);
            this.setText("<html>" + text + "</html>");
        }
        return this;
    }

}
