package com.insidious.plugin.util;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

class StringUtilsTest {

    @Test
    void testIsAllUpperCase() {
        String a = "ABCD";
        String b = "abcd";
        String c = "aBCD";
        String d = "Abcd";
        String e = "ABCD_MY_IS_HELLO";

        Assert.assertTrue(StringUtils.isAllUpperCamelCase(a));
        Assert.assertFalse(StringUtils.isAllUpperCamelCase(b));
        Assert.assertFalse(StringUtils.isAllUpperCamelCase(c));
        Assert.assertFalse(StringUtils.isAllUpperCamelCase(d));
        Assert.assertFalse(StringUtils.isAllUpperCamelCase(e));
    }

    @Test
    void testCapitalize() {
        String a = "ABCD";
        String b = "aasdasdpSed";
        String c = "aBCDefief";
        String d = "AbcdNJSDiaj";
        a = StringUtils.capitalize(a);
        b = StringUtils.capitalize(b);
        c = StringUtils.capitalize(c);
        d = StringUtils.capitalize(d);

        Assert.assertEquals("Abcd", a);
        Assert.assertEquals("Aasdasdpsed", b);
        Assert.assertEquals("Abcdefief", c);
        Assert.assertEquals("Abcdnjsdiaj", d);
    }

    @Test
    void testIsEmpty() {
        String a = null;
        String b = "";
        String c = "abce";
        String d = "a";

        Assert.assertTrue(StringUtils.isEmpty(a));
        Assert.assertTrue(StringUtils.isEmpty(b));
        Assert.assertFalse(StringUtils.isEmpty(c));
        Assert.assertFalse(StringUtils.isEmpty(d));
    }

    @Test
    void testConvertToSnakeCaseToCamelCase() {
        String a = "kuch_toh_hua_hai";
        String b = "KUCH_TOH_HUA_HAI";
        String c = "MONDAY";
        String d = "WHAT_THE_HECK";

        a = StringUtils.convertToSnakeCaseToCamelCase(a);
        b = StringUtils.convertToSnakeCaseToCamelCase(b);
        c = StringUtils.convertToSnakeCaseToCamelCase(c);
        d = StringUtils.convertToSnakeCaseToCamelCase(d);

        Assert.assertEquals("kuchTohHuaHai", a);
        Assert.assertEquals("kuchTohHuaHai", b);
        Assert.assertEquals("monday", c);
        Assert.assertEquals("whatTheHeck", d);
    }
}