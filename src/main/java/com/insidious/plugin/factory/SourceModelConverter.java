package com.insidious.plugin.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.upload.SourceModel;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public class SourceModelConverter extends Converter<SourceModel> {


    @Override
    public @Nullable SourceModel fromString(@Nullable @NonNls String s) {
        try {
            return ObjectMapperInstance.getInstance().readValue(s, SourceModel.class);
        } catch (JsonProcessingException e) {
            return new SourceModel();
        }
//        return null;
    }

    @Override
    public @Nullable String toString(@Nullable SourceModel insidiousConfigurationState) {
        try {
            return ObjectMapperInstance.getInstance().writeValueAsString(insidiousConfigurationState);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
