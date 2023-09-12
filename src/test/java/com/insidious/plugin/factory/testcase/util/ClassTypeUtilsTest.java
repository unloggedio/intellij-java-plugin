package com.insidious.plugin.factory.testcase.util;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ClassTypeUtilsTest {
    @Test
    void testCreateTypeFromNameString_PrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("I");
        Assertions.assertEquals(TypeName.class, typeName.getClass());
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
        Assertions.assertEquals(TypeName.class, typeName.getClass());
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
        Assertions.assertEquals(ArrayTypeName.class, typeName.getClass());
        Assertions.assertEquals("java.lang.Long[]", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_NonPrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("java.lang.String");
        Assertions.assertEquals(ClassName.class, typeName.getClass());
        Assertions.assertEquals("java.lang.String", typeName.toString());
    }

    @Test
    void testCreateTypeFromNameString_ArrayOfNonPrimitiveType() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("java.lang.String[]");
        // Class Type Should not be ClassName
        Assertions.assertEquals(ArrayTypeName.class, typeName.getClass());
        Assertions.assertEquals("java.lang.String[]", typeName.toString());
    }

    @Test
    void testBadlyNamedClass() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("com.package.name.BadPackage.dadlyNamed.ActualClassName");
        Assertions.assertTrue(typeName instanceof ClassName);
        ClassName className = (ClassName) typeName;
        Assertions.assertEquals("ActualClassName", className.simpleName());
        Assertions.assertEquals("com.package.name.BadPackage.dadlyNamed", className.packageName());
    }

    @Test
    void testBadlyNamedClass1() {
        TypeName typeName = ClassTypeUtils.createTypeFromNameString("com.example.WebFluxDemo.models.Task");
        Assertions.assertTrue(typeName instanceof ClassName);
        ClassName className = (ClassName) typeName;
        Assertions.assertEquals("Task", className.simpleName());
        Assertions.assertEquals("com.example.WebFluxDemo.models", className.packageName());
    }
}