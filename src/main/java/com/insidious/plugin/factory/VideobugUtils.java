package com.insidious.plugin.factory;

import com.intellij.openapi.util.text.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

public class VideobugUtils {

    public static String addAgentToVMParams(String currentVMParams, String javaAgentString) {
        if (StringUtil.isEmpty(currentVMParams)) {
            return javaAgentString;
        }
        String[] currentParams = currentVMParams.split(" ");
        ArrayList<String> vmParamList = new ArrayList<>(Arrays.asList(javaAgentString.split(" ")));

        for (String currentParameter : currentParams) {
            if (!vmParamList.contains(currentParameter) && !currentParameter.contains("unlogged")) {
                vmParamList.add(currentParameter);
            }
        }
        return String.join(" ", vmParamList);

    }
}
