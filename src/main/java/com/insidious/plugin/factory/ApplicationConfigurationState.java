package com.insidious.plugin.factory;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "com.insidious.plugin.factory.ApplicationConfigurationState",
        storages = @Storage("InsidiousApplicationPlugin.xml")
)
public class ApplicationConfigurationState implements PersistentStateComponent<ApplicationConfigurationState> {


    private boolean hasShownFeatures = false;
    public boolean hasShownFeatures() {
        return hasShownFeatures;
    }

    public boolean setShownFeatures() {
        return hasShownFeatures = true;
    }

    public ApplicationConfigurationState() {
    }

    @Override
    public ApplicationConfigurationState getState() {
        return this;
    }

    @Override
    public void loadState(ApplicationConfigurationState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}