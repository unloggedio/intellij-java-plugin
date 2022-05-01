package com.insidious.plugin.factory;

import com.intellij.openapi.util.text.StringUtil;

import java.util.Arrays;
import java.util.HashSet;

public class VideobugUtils {

    public static String addAgentToVMParams(String currentVMParams, String javaAgentString) {
        if (StringUtil.isEmpty(currentVMParams)) {
            return javaAgentString;
        }
        String[] currentParams = currentVMParams.split(" ");
        HashSet<String> vmParamList = new HashSet<>(Arrays.asList(javaAgentString.split(" ")));

        for (String vmParamPart : currentParams) {
            if (!vmParamList.contains(vmParamPart) && !vmParamPart.contains("videobug")) {
                vmParamList.add(vmParamPart);
            }
        }
        return String.join(" ", vmParamList);

    }
}
