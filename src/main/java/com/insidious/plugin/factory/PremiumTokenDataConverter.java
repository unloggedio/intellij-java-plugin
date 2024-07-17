package com.insidious.plugin.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.insidious.plugin.client.pojo.PremiumTokenData;
import com.insidious.plugin.util.ObjectMapperInstance;
import com.intellij.util.xmlb.Converter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public class PremiumTokenDataConverter extends Converter<PremiumTokenData> {

    @Override
    public @Nullable PremiumTokenData fromString(@Nullable @NonNls String s) {
        try {
            return ObjectMapperInstance.getInstance().readValue(s, PremiumTokenData.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public @Nullable String toString(@Nullable PremiumTokenData premiumTokenData) {
        try {
            return ObjectMapperInstance.getInstance().writeValueAsString(premiumTokenData);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
