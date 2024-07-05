package com.insidious.plugin.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public class FilterModelConverter extends Converter<StompFilterModel> {


    @Override
    public @Nullable StompFilterModel fromString(@Nullable @NonNls String s) {
        try {
            return ObjectMapperInstance.getInstance().readValue(s, StompFilterModel.class);
        } catch (JsonProcessingException e) {
            return new StompFilterModel();
        }
//        return null;
    }

    @Override
    public @Nullable String toString(@Nullable StompFilterModel insidiousConfigurationState) {
        try {
            return ObjectMapperInstance.getInstance().writeValueAsString(insidiousConfigurationState);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
