package com.insidious.plugin.factory;

import com.insidious.plugin.client.pojo.ExecutionSession;
import com.insidious.plugin.constants.ExecutionSessionSourceMode;
import com.insidious.plugin.ui.library.LibraryFilterState;
import com.insidious.plugin.ui.stomp.StompFilterModel;
import com.insidious.plugin.upload.ExecutionSessionSource;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;

import java.util.HashMap;
import java.util.Map;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "com.insidious.plugin.factory.InsidiousConfigurationState",
        storages = @Storage("InsidiousConfiguration.xml")
)
@Service(Service.Level.PROJECT)
public final class InsidiousConfigurationState
        implements PersistentStateComponent<InsidiousConfigurationState> {

    private final Map<String, Boolean> classFieldMockActiveStatus = new HashMap<>();
    private final Map<String, Boolean> mockActiveStatus = new HashMap<>();

    @OptionTag(converter = LibraryFilterModelConverter.class)
    private final LibraryFilterState libraryFilterModel = new LibraryFilterState();

    @OptionTag(converter = FilterModelConverter.class)
    private StompFilterModel stompFilterModel = new StompFilterModel();

    @OptionTag(converter = ExecutionSessionSourceConverter.class)
    private ExecutionSessionSource executionSessionSource =
            new ExecutionSessionSource(ExecutionSessionSourceMode.LOCAL);

    @OptionTag(converter = ExecutionSessionConverter.class)
    private ExecutionSession executionSession;


    public InsidiousConfigurationState() {
    }

    public LibraryFilterState getLibraryFilterModel() {
        return libraryFilterModel;
    }

    public StompFilterModel getFilterModel() {
        if (stompFilterModel == null) {
            stompFilterModel = new StompFilterModel();
        }
        return stompFilterModel;
    }

    public ExecutionSessionSource getSourceModel() {
        return this.executionSessionSource;
    }

    public void setSourceModel(ExecutionSessionSource executionSessionSource) {
        this.executionSessionSource = executionSessionSource;
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

    public boolean isMockActive(String declaredMockIds) {
        return mockActiveStatus.containsKey(declaredMockIds);
    }

    public void markMockDisable(String key) {
        classFieldMockActiveStatus.remove(key);
    }

    public void markMockActive(String key) {
        classFieldMockActiveStatus.put(key, true);
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

    public ExecutionSession getExecutionSession() {
        return executionSession;
    }

    public void setExecutionSession(ExecutionSession executionSession) {
        this.executionSession = executionSession;
    }
}