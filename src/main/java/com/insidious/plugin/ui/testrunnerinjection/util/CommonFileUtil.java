package com.insidious.plugin.ui.testrunnerinjection.util;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for common file operations related to UnloggedTestRunner file injection.
 */
public class CommonFileUtil {

    /**
     * Constructs the path to the test runner directory based on the module path.
     * Handles cases where some modules get saved as .../module/module.iml and others as ../module.iml
     * Handles cases when project name is different from git repository
     *
     * @param modulePath the path of the module file (.iml)
     * @return the desired directory path for the test runner files
     */
    public static String constructTestRunnerDirectoryPath(String modulePath) {
        Path path = Paths.get(modulePath);
        String moduleName = path.getFileName().toString().replace(".iml", "");
        String parentDir = path.getParent().toString();
        if (parentDir.endsWith(moduleName)) {
            parentDir = Paths.get(parentDir).getParent().toString();
        }

        String desiredDirectoryPath;
        if(new File(parentDir + "/" + moduleName).exists()) {
            desiredDirectoryPath = parentDir + "/" + moduleName + "/src/test/java";
        } else {
            desiredDirectoryPath = parentDir + "/src/test/java";
        }
        return desiredDirectoryPath;
    }
}
