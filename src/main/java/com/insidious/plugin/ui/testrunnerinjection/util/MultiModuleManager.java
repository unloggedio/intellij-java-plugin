package com.insidious.plugin.ui.testrunnerinjection.util;

import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static java.io.File.separator;

/**
 * The MultiModuleManager class provides utilities for populating
 * module checkboxes and checking for specific files.
 */
public class MultiModuleManager {

    private final Project project;
    private static final ProjectSystemId GRADLE_SYSTEM_ID = new ProjectSystemId("GRADLE");
    private final String testFilePath = separator + "src"+ separator + "test" + separator+ "java" + separator + "UnloggedTest.java";

    /**
     * Constructor for MultiModuleManager.
     *
     * @param project the current IntelliJ IDEA project
     */
    public MultiModuleManager(Project project) {
        this.project = project;
    }

    /**
     * Populates an array of JCheckBox components with the modules in the project.
     * For single-module projects, it returns a single disabled checkbox.
     *
     * @return an array of JCheckBox components
     */
    public JCheckBox[] populateCheckComboBoxWithModules() {
        Map<Module, Boolean> moduleMap = prepareModuleMap();

        // For single module project specific drop-down UI
        if (moduleMap.size() == 1) {
            Map.Entry<Module, Boolean> singleEntry = moduleMap.entrySet().iterator().next();
            JCheckBox checkBox = new JCheckBox(singleEntry.getKey().getName());
            checkBox.setSelected(true); // Set selected to be true always
            checkBox.setEnabled(false);  // Always set to disabled
            return new JCheckBox[]{checkBox};
        }

        return moduleMap.entrySet().stream()
                .map(entry -> {
                    JCheckBox checkBox = new JCheckBox(entry.getKey().getName());
                    checkBox.setEnabled(entry.getValue());// Set enabled state based on Boolean value
                    checkBox.setSelected(!entry.getValue());// If it is disabled it should be set to checked
                    return checkBox;
                })
                .toArray(JCheckBox[]::new);
    }

    /**
     * Prepares a map of modules with a boolean indicating if they lack the test runner file.
     *
     * @return a map with modules as keys and booleans as values
     */
    public Map<Module, Boolean> prepareModuleMap() {
        Map<Module, Boolean> moduleMap = new HashMap<>();
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        for (Module module : modules) {
            if (ExternalSystemApiUtil.isExternalSystemAwareModule(GRADLE_SYSTEM_ID, module) &&
                    (module.getName().endsWith(".main") || module.getName().endsWith(".test"))) {
                continue;
            }

            boolean needsInjection = !checkModuleForRunnerFile(module);
            moduleMap.put(module, needsInjection);
        }
        return moduleMap;
    }

    /**
     * Checks if a specific test runner file exists within a module.
     *
     * @param module the module to check
     * @return true if the file exists, false otherwise
     */
    private boolean checkModuleForRunnerFile(Module module) {
        boolean fileExists = false;
        VirtualFile guessedModuleDir = ProjectUtil.guessModuleDir(module);
        String testRunnerFilePath;
        if(guessedModuleDir == null) {
            testRunnerFilePath = project.getBasePath() + testFilePath;
        } else {
            testRunnerFilePath = guessedModuleDir.getPath() + testFilePath;
        }

        File testRunnerFile = new File(testRunnerFilePath);
        if (testRunnerFile.exists()) {
            fileExists = true;
        }
        return fileExists;
    }
}
