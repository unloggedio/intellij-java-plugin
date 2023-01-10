package com.insidious.plugin.factory.testcase.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClassTypeUtilsTest {
    @Test
    void testCreateTypeFromNameString_PrimitiveType() {
        String typeName = ClassTypeUtils.createTypeFromNameString("I");
        Assertions.assertEquals("int", typeName);
    }

    @Test
    void testCreateTypeFromNameString_ArrayPrimitiveType() {
        String typeName = ClassTypeUtils.createTypeFromNameString("B[]");
        Assertions.assertEquals("byte[]", typeName);
    }

    @Test
    void testCreateTypeFromNameString_FloatPrimitiveType() {
        String typeName = ClassTypeUtils.createTypeFromNameString("F[]");
        Assertions.assertEquals("float[]", typeName);
    }

    @Test
    void testCreateTypeFromNameString_SimpleBoolPrimitiveType() {
        String typeName = ClassTypeUtils.createTypeFromNameString("Z");
        Assertions.assertEquals("boolean", typeName);
    }

    @Test
    void testCreateTypeFromNameString_SimpleBoolPrimitiveArrayType() {
        String typeName = ClassTypeUtils.createTypeFromNameString("Z[]");
        Assertions.assertEquals("boolean[]", typeName);
    }

    @Test
    void testCreateTypeFromNameString_BoxedPrimitiveType() {
        String typeName = ClassTypeUtils.createTypeFromNameString("java.lang.Long");
        Assertions.assertEquals("java.lang.Long", typeName);
    }

    @Test
    void testCreateTypeFromNameString_ArrayofBoxedPrimitiveType() {
        String typeName = ClassTypeUtils.createTypeFromNameString("java.lang.Long[]");
        Assertions.assertEquals("java.lang.Long[]", typeName);
    }

    @Test
    void testCreateTypeFromNameString_NonPrimitiveType() {
        String typeName = ClassTypeUtils.createTypeFromNameString("java.lang.String");
        Assertions.assertEquals("java.lang.String", typeName);
    }

    @Test
    void testCreateTypeFromNameString_ArrayOfNonPrimitiveType() {
        String typeName = ClassTypeUtils.createTypeFromNameString("java.lang.String[]");
        Assertions.assertEquals("java.lang.String[]", typeName);
    }
}