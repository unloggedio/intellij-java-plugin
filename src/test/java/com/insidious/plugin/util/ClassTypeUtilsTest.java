package com.insidious.plugin.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassTypeUtilsTest {

    @Test
    void getDescriptorName() {

        String name = DescriptorParser.getDescriptorName("java.util.List<com.org.Profile>");
        assertEquals("Ljava/util/List<Lcom/org/Profile;>;", name);
    }
}