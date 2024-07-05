package com.insidious.plugin.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.insidious.plugin.upload.ExecutionSessionSource;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public class ExecutionSessionSourceConverter extends Converter<ExecutionSessionSource> {


    @Override
    public @Nullable ExecutionSessionSource fromString(@Nullable @NonNls String s) {
        try {
            return ObjectMapperInstance.getInstance().readValue(s, ExecutionSessionSource.class);
        } catch (JsonProcessingException e) {
            return new ExecutionSessionSource();
        }
//        return null;
    }

    @Override
    public @Nullable String toString(@Nullable ExecutionSessionSource insidiousConfigurationState) {
        try {
            return ObjectMapperInstance.getInstance().writeValueAsString(insidiousConfigurationState);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
