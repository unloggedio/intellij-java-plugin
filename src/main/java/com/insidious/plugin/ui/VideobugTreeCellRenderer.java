package com.insidious.plugin.ui;

import com.insidious.plugin.client.TestCandidateMethodAggregate;
import com.insidious.plugin.client.VideobugTreeClassAggregateNode;
import com.insidious.plugin.client.VideobugTreePackageAggregateNode;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.extension.InsidiousRunConfigTypeInterface;
import com.insidious.plugin.factory.InsidiousService;
import com.intellij.openapi.util.IconLoader;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Calendar;

import static com.intellij.util.IconUtil.createImageIcon;

public class VideobugTreeCellRenderer implements TreeCellRenderer {
    private static final String SPAN_FORMAT = "<span style='color:%s;'>%s</span>";
    private final DefaultTreeCellRenderer fallback;
    private final Icon offlineIcon;
    private final Icon methodIcon;
    private final Icon classIcon;
    private final Icon packageIcon;
    private final Icon sessionIcon;
    private InsidiousService insidiousService;
    private Icon onlineIcon;
    private Icon errorIcon;

    public VideobugTreeCellRenderer(InsidiousService insidiousService) {
        this.insidiousService = insidiousService;
        this.fallback = new DefaultTreeCellRenderer();

        this.onlineIcon = IconLoader.getIcon("icons/png/outline-screen-disabled.png", VideobugTreeCellRenderer.class);
        this.offlineIcon = IconLoader.getIcon("icons/png/outline-screen.png", VideobugTreeCellRenderer.class);
        this.methodIcon = IconLoader.getIcon("icons/png/method_v1.png", VideobugTreeCellRenderer.class);
        this.sessionIcon = IconLoader.getIcon("icons/png/icons8-folder-14.png",
                VideobugTreeCellRenderer.class);
        this.packageIcon = IconLoader.getIcon("icons/png/package_v1.png",
                VideobugTreeCellRenderer.class);
        this.classIcon = IconLoader.getIcon("icons/png/class_v1.png",
                VideobugTreeCellRenderer.class);
        this.errorIcon = IconLoader.getIcon("/icons/png/load_Error.png",
                VideobugTreeCellRenderer.class);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object userObject, boolean sel, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {

        Component component;
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();

        if (userObject instanceof ProbeInfoModel) {
            ProbeInfoModel probeInfoModel = (ProbeInfoModel) userObject;
//            String text = String.format(SPAN_FORMAT, "#d6caf0", probeInfoModel);
            String text = probeInfoModel.toString();
//            text += " [" + String.format(SPAN_FORMAT, "orange", pp.getRole()) + "]";
            renderer.setText("<html>" + text + "</html>");
//            this.setIcon(employeeIcon);
        } else if (userObject instanceof TreeClassInfoModel) {
            TreeClassInfoModel classInfoModel = (TreeClassInfoModel) userObject;
//            String text = String.format(SPAN_FORMAT, "#aba497", classInfoModel);
            String text = classInfoModel.toString();
            renderer.setText("<html>" + text + "</html>");
            renderer.setIcon(classIcon);
        } else if (userObject instanceof ExecutionSession) {
            ExecutionSession executionSession = (ExecutionSession) userObject;
//            String text = String.format(SPAN_FORMAT, "#aba497", executionSession);
            String text = executionSession.toString();
            renderer.setIcon(this.sessionIcon);
            renderer.setText("<html>" + text + "</html>");
        } else if (userObject instanceof MethodInfoModel) {
            MethodInfoModel methodInfoModel  = (MethodInfoModel) userObject;
//            String text = String.format(SPAN_FORMAT, "#719775", methodInfoModel);
            String text = methodInfoModel.toString();
            renderer.setText("<html>" + text + "</html>");
            renderer.setIcon(methodIcon);
        } else if (userObject instanceof PackageInfoModel) {
            PackageInfoModel packageInfoModel  = (PackageInfoModel) userObject;
            String text = String.format(SPAN_FORMAT, "#719775", packageInfoModel);
            renderer.setText("<html>" + text + "</html>");
            renderer.setIcon(packageIcon);
        }
        else if(userObject instanceof VideobugTreePackageAggregateNode)
        {
            renderer.setClosedIcon(packageIcon);
            renderer.setOpenIcon(packageIcon);
        }
        else if(userObject instanceof VideobugTreeClassAggregateNode)
        {
            renderer.setClosedIcon(classIcon);
            renderer.setOpenIcon(classIcon);
        }
        else if(userObject instanceof TestCandidateMethodAggregate)
        {
            renderer.setLeafIcon(methodIcon);
        }
        else if(userObject instanceof DefaultMutableTreeNode)
        {
            renderer.setLeafIcon(errorIcon);
        }
        else {
            String text = String.format("%s", userObject);
            //renderer.setText("<html>" + text + "</html>");
        }

        component = renderer.getTreeCellRendererComponent(tree, userObject, sel, expanded, leaf, row, hasFocus);
        return component;
    }

}
