package com.insidious.plugin.auto.autoCIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParseUtils {
    public static Map<String, List<String>> getTypeAndParameter(String input) {
        List<String> types = new ArrayList<>();
        List<String> parameters = new ArrayList<>();
        String[] argumentPairs = input.split("\\n");

        for (int i = 0; i < argumentPairs.length; i++) {
            String argument = argumentPairs[i];
            if (argument.equals("]")) {
                continue;
            }
            String parts[] = argument.split(":", 2);
            types.add(cleanText(parts[0]));
            parameters.add(cleanText(parts[1]));
        }
        HashMap<String, List<String>> typesAndParams = new HashMap<>();
        types.set(types.size() - 1, cleanLast(types.get(types.size() - 1)));
        parameters.set(types.size() - 1, cleanLast(parameters.get(parameters.size() - 1)));
        typesAndParams.put("types", types);
        System.out.println("Types : " + types);
        typesAndParams.put("parameters", parameters);
        System.out.println("Parameters : " + parameters);
        return typesAndParams;
    }

    private static String cleanText(String text) {
        if (text.startsWith("[")) {
            text = text.substring(1);
        }
        if (text.startsWith(",")) {
            text = text.substring(1);
        }
        return text.trim();
    }

    private static String cleanLast(String text) {
        if (text.endsWith(",")) {
            text = text.substring(0, text.length() - 1).trim();
        }
        return text;
    }
}
