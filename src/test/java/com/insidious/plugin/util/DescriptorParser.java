package com.insidious.plugin.util;

public class DescriptorParser {

    public static String getDescriptorName(String className) {
        if (className == null) {
            return "V";
        }
        if (className.length() < 2) {
            return className;
        }
        if (className.endsWith("[]")) {
            return "[" + getDescriptorName(className.substring(0, className.length() - 2));
        }
        String containerClassName = className;
        if (className.contains("<")) {
            StringBuilder nameBuilder = new StringBuilder();
            String[] classNameTemplateParts = className.split("<");
            nameBuilder.append(classNameTemplateParts[0]);
            for (int j = 1; j < classNameTemplateParts.length; j++) {
                String classNameTemplatePart = classNameTemplateParts[j];
                String[] subParts = classNameTemplatePart.split(",");
                if (subParts.length > 0) {
                    nameBuilder.append("<");
                }
                for (int i = 0; i < subParts.length; i++) {
                    String subPart = subParts[i].split(">")[0];
                    if (subPart.length() > 1) {
                        subPart = "L" + subPart.replace("/", ".") + ";";
                    }
                    if (i > 1) {
                        nameBuilder.append(", ");
                    }
                    nameBuilder.append(subPart);
                }
                if (subParts.length > 0) {
                    nameBuilder.append(">");
                }

            }

            containerClassName = nameBuilder.toString();
        }
        return "L" + containerClassName + ";";
    }

}
