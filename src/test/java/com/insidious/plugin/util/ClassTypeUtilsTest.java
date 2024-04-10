package com.insidious.plugin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassTypeUtilsTest {

    @Test
    void getDescriptorName() {

        String name = DescriptorParser.getDottedToDescriptorName("java.util.List<com.org.Profile>");
        assertEquals("Ljava/util/List<Lcom/org/Profile;>;", name);
    }

    @Test
    void getDottedClassName() {
        String dotName = ClassTypeUtils.getDottedClassName("Ljava/util/List<Lcom/org/Profile;>;");
        assertEquals("java.util.List<com.org.Profile>", dotName);
    }

    @Test
    void getJavaClassName() {
    }
}