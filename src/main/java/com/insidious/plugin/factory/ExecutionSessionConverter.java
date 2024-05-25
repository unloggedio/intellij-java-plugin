package com.insidious.plugin.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import com.insidious.plugin.client.pojo.ExecutionSession;

public class ExecutionSessionConverter extends Converter<ExecutionSession> {
    @Override
    public @Nullable ExecutionSession fromString(@Nullable @NonNls String s) {
        try {
            return ObjectMapperInstance.getInstance().readValue(s, ExecutionSession.class);
        } catch (JsonProcessingException e) {
            return null;
        }
//        return null;
    }

    @Override
    public @Nullable String toString(@Nullable ExecutionSession insidiousConfigurationState) {
        try {
            return ObjectMapperInstance.getInstance().writeValueAsString(insidiousConfigurationState);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
