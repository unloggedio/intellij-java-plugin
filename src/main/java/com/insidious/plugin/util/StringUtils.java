package com.insidious.plugin.util;

import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.Function;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;

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

    @Contract(pure = true)
    public static @NotNull String join(@NotNull Collection<String> strings, @NotNull String separator) {
        if (strings.size() <= 1) {
            return notNullize(strings.isEmpty() ? null : strings.iterator()
                    .next());
        }
        StringBuilder result = new StringBuilder();
        join(strings, separator, result);
        return result.toString();
    }

    @Contract(pure = true)
    public static @NotNull String notNullize(@Nullable String s) {
        return StringUtilRt.notNullize(s);
    }

    @Contract(pure = true)
    public static @NotNull String notNullize(@Nullable String s, @NotNull String defaultValue) {
        return StringUtilRt.notNullize(s, defaultValue);
    }

    public static void join(@NotNull Collection<String> strings, @NotNull String separator, @NotNull StringBuilder result) {
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

    @Contract(pure = true)
    public static @NotNull String join(final int @NotNull [] strings, final @NotNull String separator) {
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (i > 0) result.append(separator);
            result.append(strings[i]);
        }
        return result.toString();
    }

    @Contract(pure = true)
    public static @NotNull String join(final String @NotNull ... strings) {
        if (strings.length == 0) return "";

        final StringBuilder builder = new StringBuilder();
        for (final String string : strings) {
            builder.append(string);
        }
        return builder.toString();
    }


    @Contract(pure = true)
    public static @NotNull <T> String join(@NotNull Iterable<? extends T> items,
                                           @NotNull Function<? super T, ? extends CharSequence> f,
                                           @NotNull String separator) {
        StringBuilder result = new StringBuilder();
        join(items, f, separator, result);
        return result.toString();
    }

    public static <T> void join(@NotNull Iterable<? extends T> items,
                                @NotNull Function<? super T, ? extends CharSequence> f,
                                @NotNull String separator,
                                @NotNull StringBuilder result) {
        boolean isFirst = true;
        for (T item : items) {
            CharSequence string = f.fun(item);
            if (!isEmpty(string)) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    result.append(separator);
                }
                result.append(string);
            }
        }
    }

    @Contract(pure = true)
    public static @NotNull <T> String join(T @NotNull [] items, @NotNull Function<? super T, String> f, @NotNull String separator) {
        return join(Arrays.asList(items), f, separator);
    }

    @Contract(pure = true)
    public static @NotNull String join(@NotNull Iterable<?> items, @NotNull String separator) {
        StringBuilder result = new StringBuilder();
        for (Object item : items) {
            result.append(item)
                    .append(separator);
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
}
