package com.insidious.plugin.factory;

import com.insidious.plugin.ui.stomp.FilterModel;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;

import java.util.HashMap;
import java.util.Map;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "com.insidious.plugin.factory.InsidiousConfigurationState",
        storages = @Storage("InsidiousPlugin.xml")
)
public class InsidiousConfigurationState implements PersistentStateComponent<InsidiousConfigurationState> {

    private Map<String, Boolean> classFieldMockActiveStatus = new HashMap<>();
    private Map<String, Boolean> mockActiveStatus = new HashMap<>();
    private FilterModel filterModel = new FilterModel();

    public InsidiousConfigurationState() {
    }

    public FilterModel getFilterModel() {
        return filterModel;
    }

    @Override
    public InsidiousConfigurationState getState() {
        return this;
    }

    @Override
    public void loadState(InsidiousConfigurationState state) {
        XmlSerializerUtil.copyBean(state, this);
    }


    public void removeMock(String id) {
        mockActiveStatus.remove(id);
    }

    public void addMock(String id) {
        mockActiveStatus.put(id, true);
    }

    public boolean isActiveMock(String declaredMockIds) {
        return mockActiveStatus.containsKey(declaredMockIds);
    }

    public void removeFieldMock(String key) {
        classFieldMockActiveStatus.remove(key);
    }

    public void addFieldMock(String key) {
        classFieldMockActiveStatus.put(key, true);
    }

    public Map<String, Boolean> getClassFieldMockActiveStatus() {
        return classFieldMockActiveStatus;
    }

    public void setClassFieldMockActiveStatus(Map<String, Boolean> classFieldMockActiveStatus) {
        this.classFieldMockActiveStatus = classFieldMockActiveStatus;
    }

    public Map<String, Boolean> getMockActiveStatus() {
        return mockActiveStatus;
    }

    public void setMockActiveStatus(Map<String, Boolean> mockActiveStatus) {
        this.mockActiveStatus = mockActiveStatus;
    }

    public boolean isFieldMockActive(String key) {
        return classFieldMockActiveStatus.containsKey(key);
    }


    public boolean hasShownFeatures() {
        return classFieldMockActiveStatus.containsKey("hasShownFeatures");
    }

    public void setShownFeatures() {
        classFieldMockActiveStatus.put("hasShownFeatures", true);
    }

    public void clearPermanentFieldMockSetting() {
        boolean hasShownFeatures = hasShownFeatures();
        classFieldMockActiveStatus.clear();
        if (hasShownFeatures) {
            setShownFeatures();
        }
    }
}