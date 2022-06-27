package com.insidious.plugin.extension;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.JvmMainMethodRunConfigurationOptions;
import com.intellij.execution.configurations.*;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.util.ProgramParametersUtil;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InsidiousRunConfiguration extends ApplicationConfiguration implements ConfigurationType {
    protected InsidiousRunConfiguration(String name, @NotNull Project project, @NotNull ConfigurationFactory factory) {
        super(name, project, factory);
    }

    @Override
    public boolean isBuildProjectOnEmptyModuleList() {
        return false;
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
//        JavaParametersUtil.checkAlternativeJRE(this);
//        final String className = getMainClassName();
//        if (className == null || className.length() == 0) {
//            throw new RuntimeConfigurationError(ExecutionBundle.message("no.main.class.specified.error.text"));
//        }
        ProgramParametersUtil.checkWorkingDirectoryExist(this, getProject(), getConfigurationModule().getModule());
        JavaRunConfigurationExtensionManager.checkConfigurationIsValid(this);
    }

    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {

        InsidiousApplicationState state = new InsidiousApplicationState(this, env);
        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject(), getConfigurationModule().getSearchScope()));

        return state;

//        final JavaCommandLineState state = new JavaApplicationCommandLineState<>(this, env) {
//            @Override
//            protected JavaParameters createJavaParameters() throws ExecutionException {
//                final JavaParameters params = super.createJavaParameters();
//                // After params are fully configured, additionally ensure JAVA_ENABLE_PREVIEW_PROPERTY is set,
//                // because the scratch is compiled with this feature if it is supported by the JDK
//                final Sdk jdk = params.getJdk();
//                if (jdk != null) {
//                    final JavaSdkVersion version = JavaSdk.getInstance().getVersion(jdk);
//                    if (version != null && version.getMaxLanguageLevel().isPreview()) {
//                        final ParametersList vmOptions = params.getVMParametersList();
//                        if (!vmOptions.hasParameter(JavaParameters.JAVA_ENABLE_PREVIEW_PROPERTY)) {
//                            vmOptions.add(JavaParameters.JAVA_ENABLE_PREVIEW_PROPERTY);
//                        }
//                    }
//                }
//                return params;
//            }
//
//            @Override
//            protected void setupJavaParameters(@NotNull JavaParameters params) throws ExecutionException {
//                super.setupJavaParameters(params);
////                final File scrachesOutput = JavaScratchCompilationSupport.getScratchOutputDirectory(getProject());
////                if (scrachesOutput != null) {
////                    params.getClassPath().addFirst(FileUtil.toCanonicalPath(scrachesOutput.getAbsolutePath()).replace('/', File.separatorChar));
////                }
//            }
//
//            @NotNull
//            @Override
//            protected OSProcessHandler startProcess() throws ExecutionException {
//                final OSProcessHandler handler = super.startProcess();
//                return handler;
//            }
//        };
//        state.setConsoleBuilder(TextConsoleBuilderFactory.getInstance().createBuilder(getProject(), getConfigurationModule().getSearchScope()));
//        return state;
    }

//    @NotNull
//    @Override
//    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
//        return new JavaApplicationSettingsEditor(this);
//    }

    @Override
    public @Nullable @NonNls String getId() {
        return "indisious-run-config";
    }

    @NotNull
    @Override
    protected JvmMainMethodRunConfigurationOptions getOptions() {
        return super.getOptions();
    }

    @Nullable
    @Override
    public String getDefaultTargetName() {
        return null;
    }

    @Override
    public @NotNull @Nls(capitalization = Nls.Capitalization.Title) String getDisplayName() {
        return "Insidious Time Travel";
    }

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) String getConfigurationTypeDescription() {
        return "Insidious Time travel debugger";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[0];
    }
}
