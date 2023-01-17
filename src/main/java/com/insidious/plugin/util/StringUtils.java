package com.insidious.plugin.util;

public class StringUtils {

    /**
     * <pre>
     * StringUtils.isAllUpperCamelCase(null)   = false
     * StringUtils.isAllUpperCamelCase("")     = false
     * StringUtils.isAllUpperCamelCase("ABCD")  = true
     * StringUtils.isAllUpperCamelCase("ABC_THE_SKY_IS")  = true
     * StringUtils.isAllUpperCamelCase("aBC") = false
     * </pre>
     *
     * @param cs the CharSequence to check, may be null
     * @return {@code true} if only contains uppercase characters, and is non-null
     * @since 2.5
     * @since 3.0 Changed signature from isAllUpperCase(String) to isAllUpperCase(CharSequence)
     */
    public static boolean isAllUpperCamelCase(CharSequence cs) {
        if (cs == null || isEmpty(cs)) {
            return false;
        }
        int sz = cs.length();
        for (int i = 0; i < sz; i++) {
            if (cs.equals("_"))
                continue;
            if (Character.isUpperCase(cs.charAt(i)) == false) {
                return false;
            }
        }
        return true;
    }

    /**
     * <pre>
     * Eg:
     * ABCD => Abcd
     * wHatis => Whatis;
     * HellO => Hello;
     * AbcdNJSDiaj => Abcdnjsdiaj;
     * </pre>
     *
     * @param cs
     * @return
     */
    public static String capitalize(CharSequence cs) {
        if (isEmpty(cs))
            return cs.toString();
        if (cs.length() == 1) {
            return cs.toString().toUpperCase();
        }
        String out = cs.toString();
        out = out.toLowerCase();
        String a = out.substring(0, 1).toUpperCase() + out.substring(1);
        return a;
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static String convertSnakeCaseToCamelCase(String str) {
        if (isEmpty(str))
            return str;

        String[] l = str.split("_");

        StringBuilder modified = new StringBuilder();
        for (int i = 0; i < l.length; i++) {
            if (i == 0)
                modified.append(l[i].toLowerCase());
            else
                modified.append(capitalize(l[i]));
        }
        return modified.toString();
    }
}
