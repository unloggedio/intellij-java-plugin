package com.insidious.plugin.pojo;

import com.insidious.common.weaver.ClassInfo;
import com.insidious.common.weaver.DataInfo;
import com.insidious.common.weaver.MethodInfo;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ClassWeaveInfo {
    public static final List<MethodInfo> EMPTY_LIST = List.of();
    private final List<ClassInfo> classInfoList;
    private final List<MethodInfo> methodInfoList;
    private final List<DataInfo> dataInfoList;
    private final Map<Integer, List<MethodInfo>> methodByClass;
    private final Map<Integer, List<DataInfo>> probesByMethod;


    public ClassWeaveInfo(
            List<ClassInfo> classInfoList,
            List<MethodInfo> methodInfoList,
            List<DataInfo> dataInfoList
    ) {
        this.classInfoList = classInfoList;
        this.methodInfoList = methodInfoList;
        this.dataInfoList = dataInfoList;
        methodByClass = methodInfoList.stream().collect(Collectors.groupingBy(MethodInfo::getClassId));
        probesByMethod =
                dataInfoList.stream().collect(Collectors.groupingBy(DataInfo::getMethodId));
    }

    public ClassInfo getClassInfoById(int classId) {
        for (ClassInfo classInfo : classInfoList) {
            if (classInfo.getClassId() == classId) {
                return classInfo;
            }
        }
        return null;
    }

    public MethodInfo getMethodInfoById(int methodId) {
        for (MethodInfo methodInfo : methodInfoList) {
            if (methodInfo.getMethodId() == methodId) {
                return methodInfo;
            }
        }
        return null;
    }

    public List<MethodInfo> getMethodInfoByClassId(int classId) {
        List<MethodInfo> methodInfos = methodByClass.get(classId);
        if (methodInfos == null) {
            return EMPTY_LIST;
        }
        return methodInfos;
//        List<MethodInfo> methods = new LinkedList<>();
//        for (MethodInfo methodInfo : methodInfoList) {
//            if (methodInfo.getClassId() == classId) {
//                methods.add(methodInfo);
//            }
//        }
//        return methods;
    }

    public List<DataInfo> getProbesByMethodId(int methodId) {
        List<DataInfo> dataInfos = probesByMethod.get(methodId);
        return dataInfos;
//        List<DataInfo> methods = new LinkedList<>();
//        for (DataInfo dataInfo : dataInfoList) {
//            if (dataInfo.getMethodId() == methodId) {
//                methods.add(dataInfo);
//            }
//        }
//        return methods;
    }

    public DataInfo getProbeById(int probeId) {
        List<DataInfo> methods = new LinkedList<>();
        for (DataInfo dataInfo : dataInfoList) {
            if (dataInfo.getDataId() == probeId) {
                return dataInfo;
            }
        }
        return null;
    }

    public List<ClassInfo> getClassInfoList() {
        return classInfoList;
    }

    public List<MethodInfo> getMethodInfoList() {
        return methodInfoList;
    }

    public List<DataInfo> getDataInfoList() {
        return dataInfoList;
    }
}
