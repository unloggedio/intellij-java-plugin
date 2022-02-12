package factory;

import com.intellij.execution.Executor;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.util.Factory;
import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousSettings implements RunnerAndConfigurationSettings {
    @Override
    public @NotNull ConfigurationType getType() {
        return null;
    }

    @Override
    public @NotNull ConfigurationFactory getFactory() {
        return null;
    }

    @Override
    public boolean isTemplate() {
        return false;
    }

    @Override
    public boolean isTemporary() {
        return false;
    }

    @Override
    public void storeInLocalWorkspace() {

    }

    @Override
    public boolean isStoredInLocalWorkspace() {
        return false;
    }

    @Override
    public void storeInDotIdeaFolder() {

    }

    @Override
    public boolean isStoredInDotIdeaFolder() {
        return false;
    }

    @Override
    public void storeInArbitraryFileInProject(@NonNls @NotNull String filePath) {

    }

    @Override
    public boolean isStoredInArbitraryFileInProject() {
        return false;
    }

    @Override
    public @Nullable @NonNls String getPathIfStoredInArbitraryFileInProject() {
        return null;
    }

    @Override
    public void setTemporary(boolean temporary) {

    }

    @Override
    public @NotNull RunConfiguration getConfiguration() {
        return null;
    }

    @Override
    public void setName(@NlsSafe String name) {

    }

    @Override
    public @NotNull @NlsSafe String getName() {
        return null;
    }

    @Override
    public @NotNull String getUniqueID() {
        return null;
    }

    @Override
    public <Settings extends RunnerSettings> @Nullable Settings getRunnerSettings(@NotNull ProgramRunner<Settings> runner) {
        return null;
    }

    @Override
    public @Nullable ConfigurationPerRunnerSettings getConfigurationSettings(@NotNull ProgramRunner runner) {
        return null;
    }

    @Override
    public void checkSettings(@Nullable Executor executor) throws RuntimeConfigurationException {

    }

    @Override
    public @NotNull Factory<RunnerAndConfigurationSettings> createFactory() {
        return null;
    }

    @Override
    public void setEditBeforeRun(boolean b) {

    }

    @Override
    public boolean isEditBeforeRun() {
        return false;
    }

    @Override
    public void setActivateToolWindowBeforeRun(boolean value) {

    }

    @Override
    public boolean isActivateToolWindowBeforeRun() {
        return false;
    }

    @Override
    public void setFolderName(@Nullable String folderName) {

    }

    @Override
    public @Nullable String getFolderName() {
        return null;
    }
}
