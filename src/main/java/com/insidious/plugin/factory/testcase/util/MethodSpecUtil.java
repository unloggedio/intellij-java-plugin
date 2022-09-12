package com.insidious.plugin.factory.testcase.util;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

public class MethodSpecUtil {


    public static MethodSpec createOkHttpMockCreator() {
        MethodSpec.Builder fieldInjectorMethod = MethodSpec.methodBuilder("buildOkHttpResponseFromString");
        fieldInjectorMethod.addModifiers(javax.lang.model.element.Modifier.PRIVATE);

        fieldInjectorMethod.addParameter(String.class, "responseBodyString");
        fieldInjectorMethod.addException(Exception.class);

        fieldInjectorMethod.addCode(CodeBlock.of("" +
                        "        $T responseBody = $T.create($T.parse" +
                        "(\"application/json\"), responseBodyString);\n" +
                        "        $T request = new Request.Builder().url(\"http://example.com\").build();\n" +
                        "        return new Response.Builder()\n" +
                        "                .request(request)\n" +
                        "                .protocol($T.HTTP_2)\n" +
                        "                .body(responseBody)\n" +
                        "                .code(200)\n" +
                        "                .message(\"message ?\")\n" +
                        "                .build();\n",
                ClassName.bestGuess("okhttp3.ResponseBody"),
                ClassName.bestGuess("okhttp3.ResponseBody"),
                ClassName.bestGuess("okhttp3.MediaType"),
                ClassName.bestGuess("okhttp3.Request"),
                ClassName.bestGuess("okhttp3.Protocol")
        ));

        fieldInjectorMethod.returns(ClassName.bestGuess("okhttp3.Response"));

        return fieldInjectorMethod.build();
    }


    public static MethodSpec createInjectFieldMethod() {
        MethodSpec.Builder fieldInjectorMethod = MethodSpec.methodBuilder("injectField");
        fieldInjectorMethod.addModifiers(javax.lang.model.element.Modifier.PRIVATE);
        fieldInjectorMethod.addException(Exception.class);

        fieldInjectorMethod.addParameter(Object.class, "targetInstance");
        fieldInjectorMethod.addParameter(String.class, "name");
        fieldInjectorMethod.addParameter(Object.class, "targetObject");

        fieldInjectorMethod.addCode(CodeBlock.of("" +
                        "        Class<?> aClass;\n" +
                        "        if (targetInstance instanceof Class) {\n" +
                        "            aClass = (Class) targetInstance;\n" +
                        "            while (!targetInstance.equals(Object.class)) {\n" +
                        "                try {\n" +
                        "                    $T targetField = aClass.getDeclaredField(name);\n" +
                        "                    targetField.setAccessible(true);\n" +
                        "                    targetField.set(targetInstance, targetObject);\n" +
                        "                } catch (NoSuchFieldException nsfe) {\n" +
                        "                    // nothing to set\n" +
                        "                }\n" +
                        "                aClass = aClass.getSuperclass();\n" +
                        "            }\n" +
                        "\n" +
                        "        } else {\n" +
                        "            aClass = targetInstance.getClass();\n" +
                        "\n" +
                        "            while (!aClass.equals(Object.class)) {\n" +
                        "                try {\n" +
                        "                    Field targetField = aClass.getDeclaredField(name);\n" +
                        "                    targetField.setAccessible(true);\n" +
                        "                    targetField.set(targetInstance, targetObject);\n" +
                        "                } catch (NoSuchFieldException nsfe) {\n" +
                        "                    // nothing to set\n" +
                        "                }\n" +
                        "                aClass = aClass.getSuperclass();\n" +
                        "            }\n" +
                        "\n" +
                        "        }\n",
                ClassName.bestGuess("java.lang.reflect.Field")));

        return fieldInjectorMethod.build();
    }


}
