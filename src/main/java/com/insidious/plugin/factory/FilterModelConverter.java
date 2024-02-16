package com.insidious.plugin.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.insidious.plugin.ui.stomp.FilterModel;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public class FilterModelConverter extends Converter<FilterModel> {


    @Override
    public @Nullable FilterModel fromString(@Nullable @NonNls String s) {
        try {
            return ObjectMapperInstance.getInstance().readValue(s, FilterModel.class);
        } catch (JsonProcessingException e) {
            return new FilterModel();
        }
//        return null;
    }

    @Override
    public @Nullable String toString(@Nullable FilterModel insidiousConfigurationState) {
        try {
            return ObjectMapperInstance.getInstance().writeValueAsString(insidiousConfigurationState);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
