package com.insidious.plugin.ui;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.MethodInfo;
import com.insidious.plugin.client.VideobugClientInterface;
import com.insidious.plugin.client.pojo.DataResponse;
import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.client.pojo.exceptions.APICallException;
import com.insidious.plugin.factory.InsidiousService;
import com.insidious.plugin.pojo.ClassWeaveInfo;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class VideobugTreeModel implements TreeModel {

    private final InsidiousService service;
    private final VideobugClientInterface client;
    private DataResponse<ExecutionSession> sessionList;
    private final DefaultMutableTreeNode rootNode;
    private final Map<String, ClassWeaveInfo> sessionClassWeaveMap = new HashMap<>();
    private final Map<String, List<PackageInfoModel>> sessionPackageMap = new HashMap<>();
    private final Map<String, List<TreeClassInfoModel>> sessionPackageClassMap = new HashMap<>();
    private final Map<String, List<MethodInfoModel>> sessionClassMethodMap = new HashMap<>();
    private final Map<String, List<ProbeInfoModel>> sessionMethodProbeMap = new HashMap<>();
    private List<TreeModelListener> listeners = new LinkedList<>();

    public VideobugTreeModel(InsidiousService insidiousService) {
        String sessionNodeLabel = "Sessions";
        this.service = insidiousService;
        this.client = this.service.getClient();
        try {
            this.sessionList = client.fetchProjectSessions();

        } catch (APICallException | IOException e) {
            sessionNodeLabel = "Failed to load sessions - " + e.getMessage();
        }

        rootNode = new DefaultMutableTreeNode(sessionNodeLabel);
    }

    @Override
    public Object getRoot() {
        return rootNode;
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent == rootNode) {
            return sessionList.getItems().get(index);
        }

        Class<?> nodeType = parent.getClass();

        if (nodeType.equals(ExecutionSession.class)) {
            ExecutionSession session = (ExecutionSession) parent;
            init(session);

            return sessionPackageMap.get(session.getSessionId()).get(index);


        } else if (nodeType.equals(PackageInfoModel.class)) {
            PackageInfoModel packageNode = (PackageInfoModel) parent;
            return sessionPackageClassMap.get(packageNode.getSessionId()
                    + "-" + packageNode.getPackageNamePart()).get(index);

        } else if (nodeType.equals(TreeClassInfoModel.class)) {
            TreeClassInfoModel classNode = (TreeClassInfoModel) parent;
            return sessionClassMethodMap.get(classNode.getSessionId()
                    + "-" + classNode.getClassName()).get(index);

        }  else if (nodeType.equals(MethodInfoModel.class)) {
            MethodInfoModel methodNode = (MethodInfoModel) parent;
            String sessionProbeKey =
                    methodNode.getSessionId() + "-" + methodNode.getClassName() + "-" + methodNode.getMethodName();
            List<ProbeInfoModel> probesList = sessionMethodProbeMap.get(sessionProbeKey);
            return probesList.get(index);
        }

        return null;
    }

    public void init(ExecutionSession session) {
        ClassWeaveInfo classWeaveInfo;


        if (!sessionClassWeaveMap.containsKey(session.getSessionId())) {
            classWeaveInfo = client.getSessionClassWeave(session.getSessionId());
            sessionClassWeaveMap.put(session.getSessionId(), classWeaveInfo);
        } else {
            return;
        }


        List<ClassInfo> classInfoList = classWeaveInfo.getClassInfoList();
        Map<String, PackageInfoModel> packageInfoModelMap = new HashMap<>();

        classInfoList.sort(Comparator.comparing(ClassInfo::getClassName));


        for (ClassInfo classInfo : classInfoList) {
            if (classInfo.getClassName().contains("$")) {
                continue;
            }
            String packageName = ClassNameUtils.getPackageName(classInfo.getClassName());
            if (!packageInfoModelMap.containsKey(packageName)) {
                PackageInfoModel packageNode = new PackageInfoModel(packageName, session.getSessionId());
                packageInfoModelMap.put(packageName, packageNode);
            }
            PackageInfoModel packageInfo = packageInfoModelMap.get(packageName);

            List<MethodInfoModel> methodList = classWeaveInfo
                    .getMethodInfoByClassId(classInfo.getClassId())
                    .stream().filter(
                            e -> !e.getMethodName().startsWith("<")
                                    && !e.getMethodName().contains("$")
                    ).sorted(Comparator.comparing(MethodInfo::getMethodName))
                    .map((MethodInfo t) -> {

                        List<DataInfo> methodProbes = classWeaveInfo.getProbesByMethodId(t.getMethodId());

                        String probeKey =
                                session.getSessionId() + "-" + t.getClassName() + "-" + t.getMethodName();



                        List<ProbeInfoModel> methodProbeList = methodProbes
                                .stream().map(ProbeInfoModel::new)
                                .sorted(Comparator.comparing(ProbeInfoModel::getProbeId))
                                .collect(Collectors.toList());
                        sessionMethodProbeMap.put(probeKey, methodProbeList);

                        return new MethodInfoModel(t.getMethodName(), t.getClassName(),
                                session.getSessionId());
                    })
                    .collect(Collectors.toList());
            if (methodList.size() == 0) {
                continue;
            }




            String sessionClassMethodKey = session.getSessionId() + "-"
                    + classInfo.getClassName();
            sessionClassMethodMap.put(sessionClassMethodKey, methodList);
            TreeClassInfoModel treeInfoModel = new TreeClassInfoModel(classInfo.getClassName(),
                    session.getSessionId());
            packageInfo.addClassInfo(treeInfoModel);
        }

        sessionPackageMap.put(session.getSessionId(), new ArrayList<>(packageInfoModelMap.values()));
        for (String packageName : packageInfoModelMap.keySet()) {
            PackageInfoModel classList = packageInfoModelMap.get(packageName);
            String sessionClassListKey = session.getSessionId() + "-" + packageName;
            sessionPackageClassMap.put(sessionClassListKey, classList.getChildren());

        }


    }

    @Override
    public int getChildCount(Object parent) {
        if (parent == rootNode) {
            return sessionList.getItems().size();
        }

        Class<?> nodeType = parent.getClass();

        if (nodeType.equals(ExecutionSession.class)) {
            ExecutionSession session = (ExecutionSession) parent;
            init(session);

            return sessionPackageMap.get(session.getSessionId()).size();


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
