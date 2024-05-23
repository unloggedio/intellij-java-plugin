package com.insidious.plugin.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ObjectMapperInstance {

    // Private static instance
    private static ObjectMapper instance = null;

    // Private constructor
    private ObjectMapperInstance() {
        // You can configure the ObjectMapper here if needed
    }

    // Public static method to get the instance
    public static synchronized ObjectMapper getInstance() {
        if (instance == null) {
            instance = new ObjectMapper();
            instance.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            // Additional configuration for ObjectMapper can be added here
        }
        return instance;
    }
}
