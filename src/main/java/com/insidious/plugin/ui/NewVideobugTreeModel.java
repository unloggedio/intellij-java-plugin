package com.insidious.plugin.ui;

import com.insidious.plugin.client.SessionInstance;
import com.insidious.plugin.client.TestCandidateMethodAggregate;
import com.insidious.plugin.client.VideobugTreeClassAggregateNode;
import com.insidious.plugin.client.VideobugTreePackageAggregateNode;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.factory.testcase.candidate.TestCandidateMetadata;
import com.insidious.plugin.pojo.ClassWeaveInfo;
import com.insidious.plugin.pojo.MethodCallExpression;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicatorProvider;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.util.*;
import java.util.stream.Collectors;

public class NewVideobugTreeModel implements TreeModel {

    private final DefaultMutableTreeNode rootNode;
    private final Map<String, List<PackageInfoModel>> sessionPackageMap = new HashMap<>();
    private final Map<String, List<TreeClassInfoModel>> sessionPackageClassMap = new HashMap<>();
    private final Map<String, List<TestCandidateMethodAggregate>> sessionClassMethodMap = new HashMap<>();
    private final Map<String, List<TestCandidateMetadata>> sessionMethodTestCandidateMap = new HashMap<>();
    private final Map<String, List<ProbeInfoModel>> sessionMethodProbeMap = new HashMap<>();
    private final List<TreeModelListener> listeners = new LinkedList<>();
    private final SessionInstance sessionInstance;
    private final List<VideobugTreeClassAggregateNode> videobugTreeClassAggregateNodes;
    private final Map<String, List<VideobugTreeClassAggregateNode>> classAggregatesByPackageName;
    private final List<VideobugTreePackageAggregateNode> packageAggregates;

    public NewVideobugTreeModel(SessionInstance sessionInstance) {
        String sessionNodeLabel = "Packages";
        this.sessionInstance = sessionInstance;

        rootNode = new DefaultMutableTreeNode(sessionNodeLabel);
        videobugTreeClassAggregateNodes = sessionInstance.getTestCandidateAggregates();

        packageAggregates = new LinkedList<>();

        classAggregatesByPackageName = videobugTreeClassAggregateNodes
                .stream()
                .collect(Collectors.groupingBy(VideobugTreeClassAggregateNode::getPackageName));

        for (String packageName : classAggregatesByPackageName.keySet()) {
            List<VideobugTreeClassAggregateNode> classAggregates = classAggregatesByPackageName.get(packageName);
            Integer totalCount = classAggregates.stream()
                    .map(VideobugTreeClassAggregateNode::getCount).reduce(Integer::sum).orElse(0);
            packageAggregates.add(new VideobugTreePackageAggregateNode(packageName, totalCount));
        }
        Collections.sort(packageAggregates);


    }

    @Override
    public Object getRoot() {
        return rootNode;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent == rootNode) {
            return packageAggregates.get(index);
        }

        Class<?> nodeType = parent.getClass();


        if (nodeType.equals(VideobugTreePackageAggregateNode.class)) {
            VideobugTreePackageAggregateNode packageAggregate = (VideobugTreePackageAggregateNode) parent;
            List<VideobugTreeClassAggregateNode> classAggregates = classAggregatesByPackageName.get(packageAggregate.getPackageName());
            Collections.sort(classAggregates);
            return classAggregates.get(index);
        }

        if (nodeType.equals(VideobugTreeClassAggregateNode.class)) {
            VideobugTreeClassAggregateNode classAggregateNode = (VideobugTreeClassAggregateNode) parent;
            List<TestCandidateMethodAggregate> methodAggregateList = sessionClassMethodMap.get(classAggregateNode.getClassName());
            if (methodAggregateList == null) {
                methodAggregateList = sessionInstance
                        .getTestCandidateAggregatesByClassName(classAggregateNode.getClassName());
                Collections.sort(methodAggregateList);
                sessionClassMethodMap.put(classAggregateNode.getClassName(), methodAggregateList);
            }
            return methodAggregateList.get(index);

        }


        if (nodeType.equals(ExecutionSession.class)) {
            ExecutionSession session = (ExecutionSession) parent;
            init(session);

            return sessionPackageMap.get(session.getSessionId()).get(index);

        } else if (nodeType.equals(PackageInfoModel.class)) {
            PackageInfoModel packageNode = (PackageInfoModel) parent;
            return sessionPackageClassMap.get(packageNode.getPackageNamePart()).get(index);

        } else if (nodeType.equals(TreeClassInfoModel.class)) {
            TreeClassInfoModel classNode = (TreeClassInfoModel) parent;
            return sessionClassMethodMap.get(classNode.getClassName()).get(index);

        } else if (nodeType.equals(MethodInfoModel.class)) {
            MethodInfoModel methodNode = (MethodInfoModel) parent;
            String sessionProbeKey = methodNode.getClassName() + "-" + methodNode.getMethodName();
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
            return packageAggregates.size();
        }

        Class<?> nodeType = parent.getClass();

        if (nodeType.equals(VideobugTreePackageAggregateNode.class)) {
            VideobugTreePackageAggregateNode packageAggregate = (VideobugTreePackageAggregateNode) parent;
            List<VideobugTreeClassAggregateNode> classAggregates = classAggregatesByPackageName.get(packageAggregate.getPackageName());
            return classAggregates.size();
        }

        if (nodeType.equals(VideobugTreeClassAggregateNode.class)) {
            VideobugTreeClassAggregateNode classAggregateNode = (VideobugTreeClassAggregateNode) parent;
            List<TestCandidateMethodAggregate> methodAggregateList = sessionClassMethodMap.get(classAggregateNode.getClassName());
            if (methodAggregateList == null) {
                methodAggregateList= sessionInstance
                        .getTestCandidateAggregatesByClassName(classAggregateNode.getClassName());
                sessionClassMethodMap.put(classAggregateNode.getClassName(), methodAggregateList);
            }

            return methodAggregateList.size();

        }

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
            return sessionPackageClassMap.get(packageNode.getPackageNamePart()).size();

        } else if (nodeType.equals(TreeClassInfoModel.class)) {
            TreeClassInfoModel classNode = (TreeClassInfoModel) parent;
            List<TestCandidateMetadata> existingList = sessionMethodTestCandidateMap.get(classNode.getClassName());
            return (int) existingList
                    .stream()
                    .filter(e -> !((MethodCallExpression) e.getMainMethod()).getMethodName().startsWith("<"))
                    .count();

        }

        return 0;
    }

    @Override
    public boolean isLeaf(Object node) {
        if (node == rootNode) {
            return false;
        }
        Class<?> nodeType = node.getClass();

        if (nodeType.equals(VideobugTreePackageAggregateNode.class)) {
            return false;
        }
        if (nodeType.equals(VideobugTreeClassAggregateNode.class)) {
            return false;
        }

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
            return videobugTreeClassAggregateNodes.indexOf(child);
        }

        Class<?> nodeType = parent.getClass();

        if (nodeType.equals(VideobugTreePackageAggregateNode.class)) {
            VideobugTreePackageAggregateNode packageAggregate = (VideobugTreePackageAggregateNode) parent;
            List<VideobugTreeClassAggregateNode> classAggregates = classAggregatesByPackageName.get(packageAggregate.getPackageName());
            return classAggregates.indexOf(child);
        }

        if (nodeType.equals(VideobugTreeClassAggregateNode.class)) {
            VideobugTreeClassAggregateNode classAggregateNode = (VideobugTreeClassAggregateNode) parent;
            List<TestCandidateMethodAggregate> methodAggregateList = sessionClassMethodMap.get(classAggregateNode.getClassName());
            if (methodAggregateList == null) {
                methodAggregateList= sessionInstance
                        .getTestCandidateAggregatesByClassName(classAggregateNode.getClassName());
                sessionClassMethodMap.put(classAggregateNode.getClassName(), methodAggregateList);
            }

            return methodAggregateList.indexOf(child);

        }


        if (nodeType.equals(ExecutionSession.class)) {
            ExecutionSession session = (ExecutionSession) parent;
            init(session);
            return sessionPackageMap.get(session.getSessionId()).indexOf(child);

        } else if (nodeType.equals(PackageInfoModel.class)) {
            PackageInfoModel packageNode = (PackageInfoModel) parent;
            return sessionPackageClassMap.get(packageNode.getPackageNamePart()).indexOf(child);

        } else if (nodeType.equals(TreeClassInfoModel.class)) {
            TreeClassInfoModel classNode = (TreeClassInfoModel) parent;
            return sessionClassMethodMap.get(classNode.getClassName()).indexOf(child);

        } else if (nodeType.equals(MethodInfoModel.class)) {
            MethodInfoModel methodNode = (MethodInfoModel) parent;
            String sessionProbeKey = methodNode.getClassName() + "-" + methodNode.getMethodName();
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
