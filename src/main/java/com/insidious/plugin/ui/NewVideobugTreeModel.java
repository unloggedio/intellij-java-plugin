package com.insidious.plugin.ui;

import com.insidious.plugin.client.DaoService;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.factory.testcase.TestCaseService;
import com.insidious.plugin.pojo.ClassWeaveInfo;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.project.Project;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.*;

public class NewVideobugTreeModel implements TreeModel {

    private final DefaultMutableTreeNode rootNode;
    private final Map<String, ClassWeaveInfo> sessionClassWeaveMap = new HashMap<>();
    private final Map<String, List<PackageInfoModel>> sessionPackageMap = new HashMap<>();
    private final Map<String, List<TreeClassInfoModel>> sessionPackageClassMap = new HashMap<>();
    private final Map<String, List<MethodInfoModel>> sessionClassMethodMap = new HashMap<>();
    private final Map<String, List<ProbeInfoModel>> sessionMethodProbeMap = new HashMap<>();
    private final List<TreeModelListener> listeners = new LinkedList<>();
    private final TestCaseService testCaseService;
    private final List<PackageInfoModel> packageNames;
    private DataResponse<ExecutionSession> sessionList;
    private Project project;

    public NewVideobugTreeModel(TestCaseService testCaseService) {
        String sessionNodeLabel = "Sessions";
        this.testCaseService = testCaseService;

        rootNode = new DefaultMutableTreeNode(sessionNodeLabel);

        packageNames = testCaseService.getPackageNames();
    }

    @Override
    public Object getRoot() {
        return rootNode;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent == rootNode) {
            return packageNames.get(index);
        }

        Class<?> nodeType = parent.getClass();

        if (nodeType.equals(ExecutionSession.class)) {
            ExecutionSession session = (ExecutionSession) parent;
            init(session);

            return sessionPackageMap.get(session.getSessionId()).get(index);

        } else if (nodeType.equals(PackageInfoModel.class)) {
            PackageInfoModel packageNode = (PackageInfoModel) parent;
            return sessionPackageClassMap.get(packageNode.getSessionId() + "-" + packageNode.getPackageNamePart()).get(index);

        } else if (nodeType.equals(TreeClassInfoModel.class)) {
            TreeClassInfoModel classNode = (TreeClassInfoModel) parent;
            return sessionClassMethodMap.get(classNode.getSessionId() + "-" + classNode.getClassName()).get(index);

        } else if (nodeType.equals(MethodInfoModel.class)) {
            MethodInfoModel methodNode = (MethodInfoModel) parent;
            String sessionProbeKey = methodNode.getSessionId() + "-" + methodNode.getClassName() + "-" + methodNode.getMethodName();
            List<ProbeInfoModel> probesList = sessionMethodProbeMap.get(sessionProbeKey);
            return probesList.get(index);
        }

        return null;
    }

    public void init(ExecutionSession session) {


    }

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator().isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator().setText2(text1);
            }
        }
    }


    @Override
    public int getChildCount(Object parent) {
        if (parent == rootNode) {
            return packageNames.size();
        }

        Class<?> nodeType = parent.getClass();

        if (nodeType.equals(ExecutionSession.class)) {
            ExecutionSession session = (ExecutionSession) parent;
            init(session);

            List<PackageInfoModel> packageInfoModels = sessionPackageMap.get(session.getSessionId());
            if (packageInfoModels == null) {
                return 0;
            }
            return packageInfoModels.size();


        } else if (nodeType.equals(PackageInfoModel.class)) {
            PackageInfoModel packageNode = (PackageInfoModel) parent;
            return sessionPackageClassMap.get(packageNode.getSessionId()
                    + "-" + packageNode.getPackageNamePart()).size();

        } else if (nodeType.equals(TreeClassInfoModel.class)) {
            TreeClassInfoModel classNode = (TreeClassInfoModel) parent;
            return sessionClassMethodMap.get(classNode.getSessionId()
                    + "-" + classNode.getClassName()).size();

        } else if (nodeType.equals(MethodInfoModel.class)) {
            MethodInfoModel methodNode = (MethodInfoModel) parent;
            String sessionProbeKey =
                    methodNode.getSessionId() + "-" + methodNode.getClassName() + "-" + methodNode.getMethodName();
            List<ProbeInfoModel> probesList = sessionMethodProbeMap.get(sessionProbeKey);
            return probesList.size();
        }

        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        if (node == rootNode) {
            return false;
        }
        Class<?> nodeType = node.getClass();

        if (nodeType.equals(ExecutionSession.class)) {
            return false;

        } else if (nodeType.equals(PackageInfoModel.class)) {
            return false;

        } else if (nodeType.equals(TreeClassInfoModel.class)) {
            return false;

        } else return !nodeType.equals(MethodInfoModel.class);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {

    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        if (parent == rootNode) {
            return sessionList.getItems().indexOf(child);
        }

        Class<?> nodeType = parent.getClass();

        if (nodeType.equals(ExecutionSession.class)) {
            ExecutionSession session = (ExecutionSession) parent;
            init(session);

            return sessionPackageMap.get(session.getSessionId()).indexOf(child);


        } else if (nodeType.equals(PackageInfoModel.class)) {
            PackageInfoModel packageNode = (PackageInfoModel) parent;
            return sessionPackageClassMap.get(packageNode.getSessionId()
                    + "-" + packageNode.getPackageNamePart()).indexOf(child);

        } else if (nodeType.equals(TreeClassInfoModel.class)) {
            TreeClassInfoModel classNode = (TreeClassInfoModel) parent;
            return sessionClassMethodMap.get(classNode.getSessionId()
                    + "-" + classNode.getClassName()).indexOf(child);

        } else if (nodeType.equals(MethodInfoModel.class)) {
            MethodInfoModel methodNode = (MethodInfoModel) parent;
            String sessionProbeKey =
                    methodNode.getSessionId() + "-" + methodNode.getClassName() + "-" + methodNode.getMethodName();
            List<ProbeInfoModel> probesList = sessionMethodProbeMap.get(sessionProbeKey);
            return probesList.indexOf(child);
        }

        return 0;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        this.listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        this.listeners.remove(l);
    }

}
