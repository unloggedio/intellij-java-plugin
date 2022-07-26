package com.insidious.plugin.ui;

import com.insidious.common.weaver.ClassInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.util.text.Strings;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ClassNameUtils {
    public static String getPackageName(String classInfo) {
        String[] parts;
        if (classInfo.contains(".")) {
            parts = classInfo.split("\\.");
        } else {
            parts = classInfo.split("/");
        }
        List<String> partsList = Arrays.asList(parts).subList(0, parts.length - 1);
        return StringUtil.join(partsList, ".");
    }
}
