package com.insidious.plugin.factory;

import com.intellij.openapi.util.text.StringUtil;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class VideobugUtils {

    public static String addAgentToVMParams(String currentVMParams, String javaAgentString) {
        if (StringUtil.isEmpty(currentVMParams)) {
            return javaAgentString;
        }

        String[] paramsToAdd = javaAgentString.split(" ");
        List<String> vmParamList = new LinkedList<>(Arrays.asList(currentVMParams.split(" ")));
        for (String vmParamPart : paramsToAdd) {
            if (!vmParamList.contains(vmParamPart)) {
                vmParamList.add(vmParamPart);
            }
        }
        return String.join(" ", vmParamList);

    }
}
