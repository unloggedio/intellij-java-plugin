package com.insidious.plugin.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;

public class ExceptionUtilsTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    public void testCase1() throws JsonProcessingException {

        IllegalAccessException exception = new IllegalAccessException("exception 1");
        InvocationTargetException exception2 = new InvocationTargetException(exception, "exception 2");

        String exceptionJson = objectMapper.writeValueAsString(exception2);

        String exceptionPrettyPrinted = ExceptionUtils.prettyPrintException(exceptionJson);

        System.err.println(exceptionPrettyPrinted);

    }
}