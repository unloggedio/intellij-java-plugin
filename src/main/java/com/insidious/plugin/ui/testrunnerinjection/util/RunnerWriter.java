package com.insidious.plugin.ui.testrunnerinjection.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.notification.NotificationType;
import com.insidious.plugin.InsidiousNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.Arrays;

/**
 * Utility class for writing UnloggedTestRunner file in specific module.
 */
public class RunnerWriter {

    private final Project project;

    private final Logger logger = LoggerFactory.getLogger(RunnerWriter.class);

    /**
     * Constructor for RunnerWriter.
     *
     * @param project the current IntelliJ IDEA project
     */
    public RunnerWriter(Project project) {
        this.project = project;
    }

    /**
     * Writes a test runner file for the specified module.
     *
     * @param moduleName the name of the module to write the runner file for
     */
    public void writeFile(String moduleName) {

        Module targetModule = getModuleByName(moduleName);
        if (targetModule == null) {
            InsidiousNotification.notifyMessage("Module not found: " + moduleName, NotificationType.ERROR);
            return;
        }

        File runnerFile = constructTestRunnerFile(targetModule.getModuleFilePath());
        logger.info("{} injected file exists: {}", runnerFile.getAbsolutePath(), runnerFile.exists());
        if (runnerFile.exists()) {
            InsidiousNotification.notifyMessage("Runner file for the module: "+moduleName+", already exists", NotificationType.WARNING);
            return;
        }

        // Write the contents to the runner file
        writeContentsInFile(runnerFile);

        // Refresh the Virtual File System to reflect the new file
        VirtualFileManager.getInstance()
                .refreshAndFindFileByUrl(FileSystems.getDefault().getPath(runnerFile.getAbsolutePath()).toUri().toString());
    }

    /**
     * Writes the generated content into the specified file.
     *
     * @param runnerFile the file to write the contents into
     */
    private void writeContentsInFile(File runnerFile) {
        String runnerFileContent = generateRunnerFileContent();
        try (FileOutputStream fileOutputStream = new FileOutputStream(runnerFile)) {
            fileOutputStream.write(runnerFileContent.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            InsidiousNotification.notifyMessage("Failed to write runner file: " + e.getMessage(), NotificationType.ERROR);
        }
    }

    /**
     * Retrieves a module by its name.
     *
     * @param moduleName the name of the module to retrieve
     * @return the Module object, or null if not found
     */
    private Module getModuleByName(String moduleName) {
        ModuleManager moduleManager = ModuleManager.getInstance(project);
        Module[] modules = moduleManager.getModules();
        return Arrays.stream(modules)
                .filter(module -> module.getName().equals(moduleName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Constructs the file for UnloggedTestRunner based on the module path.
     *
     * @param modulePath the path of the module file (.iml)
     * @return the File object representing the UnloggedTestRunner file.
     */
    private File constructTestRunnerFile(String modulePath) {
        String desiredDirectoryPath = CommonFileUtil.constructTestRunnerDirectoryPath(modulePath);
        File desiredDirectory = new File(desiredDirectoryPath);
        desiredDirectory.mkdirs();
        String desiredPath = desiredDirectoryPath + "/UnloggedTest.java";
        File runnerFile = new File(desiredPath);
        return runnerFile;
    }

    /**
     * Generates the content for the test runner file.
     *
     * @return the content string for the test runner file
     */
    private String generateRunnerFileContent() {
        return "import io.unlogged.runner.UnloggedTestRunner;\n" +
                "import org.junit.runner.RunWith;\n\n" +
                "@RunWith(UnloggedTestRunner.class)\n" +
                "public class UnloggedTest {\n" +
                "}\n";
    }
}
