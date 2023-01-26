// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.insidious.plugin.util;

import com.intellij.openapi.util.text.StringUtilRt;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Function;
import com.intellij.util.text.CharArrayCharSequence;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class Strings {
    public static final CharSequence EMPTY_CHAR_SEQUENCE = new CharArrayCharSequence(ArrayUtilRt.EMPTY_CHAR_ARRAY);
    private static final List<String> REPLACES_REFS = Arrays.asList("&lt;", "&gt;", "&amp;", "&#39;", "&quot;");
    private static final List<String> REPLACES_DISP = Arrays.asList("<", ">", "&", "'", "\"");

    public static boolean isAscii(char ch) {
        return ch < 128;
    }

    @Contract(pure = true)
    public static boolean isDecimalDigit(char c) {
        return c >= '0' && c <= '9';
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static String toLowerCase(@Nullable String str) {
        return str == null ? null : str.toLowerCase(Locale.ENGLISH);
    }


    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static String toUpperCase(String s) {
        return s == null ? null : s.toUpperCase(Locale.ENGLISH);
    }

    @Contract(pure = true)
    public static boolean contains(@NotNull CharSequence sequence, @NotNull CharSequence infix) {
        return indexOf(sequence, infix) >= 0;
    }

    @Contract(pure = true)
    public static char toUpperCase(char a) {
        if (a < 'a') return a;
        if (a <= 'z') return (char) (a + ('A' - 'a'));
        return Character.toUpperCase(a);
    }


    /**
     * Allows to answer if target symbol is contained at given char sequence at {@code [start; end)} interval.
     *
     * @param s     target char sequence to check
     * @param start start offset to use within the given char sequence (inclusive)
     * @param end   end offset to use within the given char sequence (exclusive)
     * @param c     target symbol to check
     * @return {@code true} if given symbol is contained at the target range of the given char sequence;
     * {@code false} otherwise
     */
    @Contract(pure = true)
    public static boolean contains(@NotNull CharSequence s, int start, int end, char c) {
        return indexOf(s, c, start, end) >= 0;
    }


    @Contract(pure = true)
    public static boolean containsChar(final @NotNull String value, final char ch) {
        return value.indexOf(ch) >= 0;
    }

    @Contract(pure = true)
    public static boolean containsAnyChar(final @NotNull String value, final @NotNull String chars) {
        return chars.length() > value.length()
                ? containsAnyChar(value, chars, 0, value.length())
                : containsAnyChar(chars, value, 0, chars.length());
    }

    @Contract(pure = true)
    public static boolean containsAnyChar(final @NotNull String value,
                                          final @NotNull String chars,
                                          final int start, final int end) {
        for (int i = start; i < end; i++) {
            if (chars.indexOf(value.charAt(i)) >= 0) {
                return true;
            }
        }

        return false;
    }

    @Contract(pure = true)
    public static int indexOf(@NotNull CharSequence s, char c) {
        return indexOf(s, c, 0, s.length());
    }

    @Contract(pure = true)
    public static int indexOf(@NotNull CharSequence s, char c, int start) {
        return indexOf(s, c, start, s.length());
    }

    @Contract(pure = true)
    public static int indexOf(@NotNull CharSequence s, char c, int start, int end) {
        end = Math.min(end, s.length());
        for (int i = Math.max(start, 0); i < end; i++) {
            if (s.charAt(i) == c) return i;
        }
        return -1;
    }

    @Contract(pure = true)
    public static int indexOf(@NotNull CharSequence sequence, @NotNull CharSequence infix) {
        return indexOf(sequence, infix, 0);
    }

    @Contract(pure = true)
    public static int indexOf(@NotNull CharSequence sequence, @NotNull CharSequence infix, int start) {
        return indexOf(sequence, infix, start, sequence.length());
    }

    @Contract(pure = true)
    public static int indexOf(@NotNull CharSequence sequence, @NotNull CharSequence infix, int start, int end) {
        for (int i = start; i <= end - infix.length(); i++) {
            if (startsWith(sequence, i, infix)) {
                return i;
            }
        }
        return -1;
    }


    @Contract(pure = true)
    public static int indexOfAny(final @NotNull String s, final @NotNull String chars) {
        return indexOfAny(s, chars, 0, s.length());
    }

    @Contract(pure = true)
    public static int indexOfAny(final @NotNull CharSequence s, final @NotNull String chars) {
        return indexOfAny(s, chars, 0, s.length());
    }

    @Contract(pure = true)
    public static int indexOfAny(final @NotNull String s, final @NotNull String chars, final int start, final int end) {
        return indexOfAny((CharSequence) s, chars, start, end);
    }

    @Contract(pure = true)
    public static int indexOfAny(final @NotNull CharSequence s, final @NotNull String chars, final int start, int end) {
        if (chars.isEmpty()) return -1;

        end = Math.min(end, s.length());
        for (int i = Math.max(start, 0); i < end; i++) {
            if (containsChar(chars, s.charAt(i))) return i;
        }
        return -1;
    }


    /**
     * Capitalize the first letter of the sentence.
     */
    @Contract(pure = true)
    public static @NotNull String capitalize(@NotNull String s) {
        if (s.isEmpty()) return s;
        if (s.length() == 1) return toUpperCase(s);
        if (Character.isUpperCase(s.charAt(0))) return s;
        return toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Contract(value = "null -> false", pure = true)
    public static boolean isCapitalized(@Nullable String s) {
        return s != null && !s.isEmpty() && Character.isUpperCase(s.charAt(0));
    }

    @Contract(pure = true)
    public static boolean startsWith(@NotNull CharSequence text, int startIndex, @NotNull CharSequence prefix) {
        int tl = text.length();
        if (startIndex < 0 || startIndex > tl) {
            throw new IllegalArgumentException("Index is out of bounds: " + startIndex + ", length: " + tl);
        }
        int l1 = tl - startIndex;
        int l2 = prefix.length();
        if (l1 < l2) return false;

        for (int i = 0; i < l2; i++) {
            if (text.charAt(i + startIndex) != prefix.charAt(i)) return false;
        }
        return true;
    }


    @Contract(value = "null -> false", pure = true)
    public static boolean isNotEmpty(@Nullable String s) {
        return !isEmpty(s);
    }

    @Contract(value = "null -> true", pure = true)
    public static boolean isEmpty(@Nullable String s) {
        return s == null || s.isEmpty();
    }

    @Contract(pure = true)
    public static @Nullable String nullize(@Nullable String s, @Nullable String defaultValue) {
        boolean empty = isEmpty(s) || Objects.equals(s, defaultValue);
        return empty ? null : s;
    }

    @Contract(value = "null -> null; !null -> !null", pure = true)
    public static String trim(@Nullable String s) {
        return s == null ? null : s.trim();
    }


    @Contract(pure = true)
    public static @NotNull String trimStart(@NotNull String s, @NotNull String prefix) {
        if (s.startsWith(prefix)) {
            return s.substring(prefix.length());
        }
        return s;
    }

    @Contract(pure = true)
    public static int stringHashCode(@NotNull CharSequence chars, int from, int to) {
        return stringHashCode(chars, from, to, 0);
    }

    @Contract(pure = true)
    public static int stringHashCode(@NotNull CharSequence chars, int from, int to, int prefixHash) {
        int h = prefixHash;
        for (int off = from; off < to; off++) {
            h = 31 * h + chars.charAt(off);
        }
        return h;
    }

    @Contract(pure = true)
    public static int stringHashCode(char @NotNull [] chars, int from, int to) {
        int h = 0;
        for (int off = from; off < to; off++) {
            h = 31 * h + chars[off];
        }
        return h;
    }

    @Contract(pure = true)
    public static int countChars(@NotNull CharSequence text, char c) {
        return countChars(text, c, 0, false);
    }

    @Contract(pure = true)
    public static int countChars(@NotNull CharSequence text, char c, int offset, boolean stopAtOtherChar) {
        return countChars(text, c, offset, text.length(), stopAtOtherChar);
    }

    @Contract(pure = true)
    public static int countChars(@NotNull CharSequence text, char c, int start, int end, boolean stopAtOtherChar) {
        boolean forward = start <= end;
        start = forward ? Math.max(0, start) : Math.min(text.length(), start);
        end = forward ? Math.min(text.length(), end) : Math.max(0, end);
        int count = 0;
        for (int i = forward ? start : start - 1; forward == i < end; i += forward ? 1 : -1) {
            if (text.charAt(i) == c) {
                count++;
            } else if (stopAtOtherChar) {
                break;
            }
        }
        return count;
    }

    public static @NotNull StringBuilder escapeToRegexp(@NotNull CharSequence text, @NotNull StringBuilder builder) {
        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            if (c == ' ' || Character.isLetter(c) || Character.isDigit(c) || c == '_') {
                builder.append(c);
            } else if (c == '\n') {
                builder.append("\\n");
            } else if (c == '\r') {
                builder.append("\\r");
            } else {
                final Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
                if (block == Character.UnicodeBlock.HIGH_SURROGATES || block == Character.UnicodeBlock.LOW_SURROGATES) {
                    builder.append(c);
                } else {
                    builder.append('\\')
                            .append(c);
                }
            }
        }

        return builder;
    }

    /**
     * @return {@code text} with some standard XML entities replaced with corresponding characters, e.g. '{@code &lt;}' replaced with '<'
     */
    @Contract(pure = true)
    public static @NotNull String unescapeXmlEntities(@NotNull String text) {
        return replace(text, REPLACES_REFS, REPLACES_DISP);
    }

    /**
     * @return {@code text} with some characters replaced with standard XML entities, e.g. '<' replaced with '{@code &lt;}'
     */
    @Contract(pure = true)
    public static @NotNull String escapeXmlEntities(@NotNull String text) {
        return replace(text, REPLACES_DISP, REPLACES_REFS);
    }

    @Contract(pure = true)
    public static @NotNull String replace(@NotNull String text, @NotNull List<String> from, @NotNull List<String> to) {
        assert from.size() == to.size();
        StringBuilder result = null;
        replace:
        for (int i = 0; i < text.length(); i++) {
            for (int j = 0; j < from.size(); j += 1) {
                String toReplace = from.get(j);
                String replaceWith = to.get(j);

                final int len = toReplace.length();
                if (len == 0) continue;
                if (text.regionMatches(i, toReplace, 0, len)) {
                    if (result == null) {
                        result = new StringBuilder(text.length());
                        result.append(text, 0, i);
                    }
                    result.append(replaceWith);
                    //noinspection AssignmentToForLoopParameter
                    i += len - 1;
                    continue replace;
                }
            }

            if (result != null) {
                result.append(text.charAt(i));
            }
        }
        return result == null ? text : result.toString();
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

    @Contract(value = "null -> true", pure = true)
    public static boolean isEmpty(@Nullable CharSequence cs) {
        return StringUtilRt.isEmpty(cs);
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
    public static boolean isWhiteSpace(char c) {
        return c == '\n' || c == '\t' || c == ' ';
    }

    @Contract(pure = true)
    public static int stringHashCodeIgnoreWhitespaces(@NotNull CharSequence chars) {
        int h = 0;
        for (int off = 0; off < chars.length(); off++) {
            char c = chars.charAt(off);
            if (!isWhiteSpace(c)) {
                h = 31 * h + c;
            }
        }
        return h;
    }

    @Contract(pure = true)
    public static boolean equalsIgnoreWhitespaces(@Nullable CharSequence s1, @Nullable CharSequence s2) {
        if (s1 == null ^ s2 == null) {
            return false;
        }

        if (s1 == null) {
            return true;
        }

        int len1 = s1.length();
        int len2 = s2.length();

        int index1 = 0;
        int index2 = 0;
        while (index1 < len1 && index2 < len2) {
            if (s1.charAt(index1) == s2.charAt(index2)) {
                index1++;
                index2++;
                continue;
            }

            boolean skipped = false;
            while (index1 != len1 && isWhiteSpace(s1.charAt(index1))) {
                skipped = true;
                index1++;
            }
            while (index2 != len2 && isWhiteSpace(s2.charAt(index2))) {
                skipped = true;
                index2++;
            }

            if (!skipped) return false;
        }

        for (; index1 != len1; index1++) {
            if (!isWhiteSpace(s1.charAt(index1))) return false;
        }
        for (; index2 != len2; index2++) {
            if (!isWhiteSpace(s2.charAt(index2))) return false;
        }

        return true;
    }

}