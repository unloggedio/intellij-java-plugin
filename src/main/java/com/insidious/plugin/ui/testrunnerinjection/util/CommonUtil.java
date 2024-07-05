package com.insidious.plugin.ui.testrunnerinjection.util;

/**
 * Common Utility Class.
 */
public class CommonUtil {

    /**
     * Generates the content for the test runner file.
     *
     * @return the content string for the test runner file
     */
    public static String generateRunnerFileContent() {
        return "import io.unlogged.runner.UnloggedTestRunner;\n" +
                "import org.junit.runner.RunWith;\n\n" +
                "@RunWith(UnloggedTestRunner.class)\n" +
                "public class UnloggedTest {\n" +
                "}";
    }
}
