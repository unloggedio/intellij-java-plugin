package com.insidious.plugin.util;

import java.util.Collection;

public class StringUtils {

    public static int getLevenshteinDistance(CharSequence s, CharSequence t) {
        if (s != null && t != null) {
            int n = s.length();
            int m = t.length();
            if (n == 0) {
                return m;
            } else if (m == 0) {
                return n;
            } else {
                if (n > m) {
                    CharSequence tmp = s;
                    s = t;
                    t = tmp;
                    n = m;
                    m = tmp.length();
                }

                int[] p = new int[n + 1];

                int i;
                for (i = 0; i <= n; p[i] = i++) {
                }

                for (int j = 1; j <= m; ++j) {
                    int upperleft = p[0];
                    char jOfT = t.charAt(j - 1);
                    p[0] = j;

                    for (i = 1; i <= n; ++i) {
                        int upper = p[i];
                        int cost = s.charAt(i - 1) == jOfT ? 0 : 1;
                        p[i] = Math.min(Math.min(p[i - 1] + 1, p[i] + 1), upperleft + cost);
                        upperleft = upper;
                    }
                }

                return p[n];
            }
        } else {
            throw new IllegalArgumentException("Strings must not be null");
        }
    }


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


    public static String join(Collection<String> strings, String separator) {
        if (strings.size() <= 1) {
            return strings.isEmpty() ? "" : strings.iterator().next();
        }
        StringBuilder result = new StringBuilder();
        join(strings, separator, result);
        return result.toString();
    }

    public static void join(Collection<String> strings, String separator, StringBuilder result) {
        boolean isFirst = true;
        for (String string : strings) {
            if (string != null) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    result.append(separator);
                }
                result.append(string);
            }
        }
    }


    public static String join(final int[] strings, final String separator) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (i > 0) result.append(separator);
            result.append(strings[i]);
        }
        return result.toString();
    }


    public static String join(final String... strings) {
        if (strings.length == 0) return "";

        final StringBuilder builder = new StringBuilder();
        for (final String string : strings) {
            builder.append(string);
        }
        return builder.toString();
    }


    public static String join(Iterable<?> items, String separator) {
        StringBuilder result = new StringBuilder();
        for (Object item : items) {
            result.append(item).append(separator);
        }
        if (result.length() > 0) {
            result.setLength(result.length() - separator.length());
        }
        return result.toString();
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

    public static int getLevenshteinDistance(String s, String t) {
        if (s != null && t != null) {
            int n = s.length();
            int m = t.length();
            if (n == 0) {
                return m;
            } else if (m == 0) {
                return n;
            } else {
                if (n > m) {
                    String tmp = s;
                    s = t;
                    t = tmp;
                    n = m;
                    m = tmp.length();
                }

                int[] p = new int[n + 1];
                int[] d = new int[n + 1];

                int i;
                for(i = 0; i <= n; p[i] = i++) {
                }

                for(int j = 1; j <= m; ++j) {
                    char t_j = t.charAt(j - 1);
                    d[0] = j;

                    for(i = 1; i <= n; ++i) {
                        int cost = s.charAt(i - 1) == t_j ? 0 : 1;
                        d[i] = Math.min(Math.min(d[i - 1] + 1, p[i] + 1), p[i - 1] + cost);
                    }

                    int[] _d = p;
                    p = d;
                    d = _d;
                }

                return p[n];
            }
        } else {
            throw new IllegalArgumentException("Strings must not be null");
        }
    }
}
