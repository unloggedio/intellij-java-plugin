package factory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supports storing the application settings in a persistent way.
 * The {@link State} and {@link Storage} annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
        name = "factory.InsidiousConfigurationState",
        storages = @Storage("InsidiousPlugin.xml")
)
public class InsidiousConfigurationState implements PersistentStateComponent<InsidiousConfigurationState> {

    public String username = "";
    public String serverUrl = "http://localhost:8080";

    public void setUsername(String username) {
        this.username = username;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public static InsidiousConfigurationState getInstance(Project project) {
        return project.getService(InsidiousConfigurationState.class);
    }

    @Nullable
    @Override
    public InsidiousConfigurationState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull InsidiousConfigurationState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

}