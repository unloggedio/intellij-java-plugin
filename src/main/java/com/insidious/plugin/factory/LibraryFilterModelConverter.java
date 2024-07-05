package com.insidious.plugin.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.insidious.plugin.ui.library.LibraryFilterState;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public class LibraryFilterModelConverter extends Converter<LibraryFilterState> {


    @Override
    public @Nullable LibraryFilterState fromString(@Nullable @NonNls String s) {
        try {
            return ObjectMapperInstance.getInstance().readValue(s, LibraryFilterState.class);
        } catch (JsonProcessingException e) {
            return new LibraryFilterState();
        }
//        return null;
    }

    @Override
    public @Nullable String toString(@Nullable LibraryFilterState insidiousConfigurationState) {
        try {
            return ObjectMapperInstance.getInstance().writeValueAsString(insidiousConfigurationState);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
