package com.insidious.plugin.factory.testcase.util;

import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClassTypeUtilsTest {
    @Test
    void testCreateTypeFromNameString_PrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("I");
        Assertions.assertEquals("int", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_ArrayPrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("B[]");
        Assertions.assertEquals("byte[]", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_FloatPrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("F[]");
        Assertions.assertEquals("float[]", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_SimpleBoolPrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("Z");
        Assertions.assertEquals("boolean", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_SimpleBoolPrimitiveArrayType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("Z[]");
        Assertions.assertEquals("boolean[]", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_BoxedPrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("java.lang.Long");
        Assertions.assertEquals("java.lang.Long", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_ArrayofBoxedPrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("java.lang.Long[]");
        Assertions.assertEquals("java.lang.Long[]", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_NonPrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("java.lang.String");
        Assertions.assertEquals("java.lang.String", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_ArrayOfNonPrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("java.lang.String[]");
        Assertions.assertEquals("java.lang.String[]", typeName.toString());
    }
}