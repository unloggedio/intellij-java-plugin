package com.insidious.plugin.util;

import com.insidious.plugin.pojo.Parameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class TestParameterUtils {

    Parameter parameter;

    @BeforeEach
    void setUp() {
        parameter = new Parameter();
        // List<Map<Int,List<Int>>>
        parameter.setName("testParam");
        parameter.setType("java.util.List");

        Parameter pMap = new Parameter();
        pMap.setName("map");
        pMap.setType("java.util.Map<java.lang.Integer,java.util.List<java.lang.Integer>>");

        Parameter pListOfInteger = new Parameter();
        pListOfInteger.setName("listOfInt");
        pListOfInteger.setType("java.util.List<java.lang.Integer>");

//        Parameter pListOfInt = new Parameter();
//        pMap.setType("java.lang.Integer");

        List<Parameter> templateMap = new ArrayList<>();

//        templateMap.add(pListOfInt);
        templateMap.add(pMap);

        parameter.setTemplateMap(templateMap);
    }

    @Test
    void testMethodNormaliseParameter() {
        ParameterUtils.normaliseParameterType(parameter);
        Assertions.assertEquals(parameter.getType(), "java.util.List");
        Assertions.assertEquals(parameter.getTemplateMap().get(0).getType(), "java.util.Map");
        Assertions.assertEquals(parameter.getTemplateMap().get(0).getTemplateMap().get(0).getType(), "java.lang.Integer");
        Assertions.assertEquals(parameter.getTemplateMap().get(0).getTemplateMap().get(1).getType(), "java.util.List");
        Assertions.assertEquals(parameter.getTemplateMap().get(0).getTemplateMap().get(1).getTemplateMap().get(0).getType(), "java.lang.Integer");
    }

    @Test
    void testMethodDeNormaliseParameterType() {
        Parameter p1 = new Parameter(1L);
        p1.setTypeForced(parameter.getType());
        ParameterUtils.normaliseParameterType(parameter);

        Parameter p2 = new Parameter(1L);
        ParameterUtils.denormalizeParameterType(p1);
        p2.setTypeForced(parameter.getType());

        Assertions.assertEquals(p1.getType(), p2.getType());
    }

    @Test
    void testMethodCreateStatementStringForParameter() {
        StringBuilder statementBuilder = new StringBuilder();
        List<Object> statementParameter = new ArrayList<>();

        ParameterUtils.normaliseParameterType(parameter);
        ParameterUtils.createStatementStringForParameter(parameter, statementBuilder, statementParameter);

        Assertions.assertEquals("$T<$T<$T, $T<$T>>>", statementBuilder.toString());
    }
}