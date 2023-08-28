package com.insidious.plugin.ui.testdesigner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.Problem;
import com.github.javaparser.ast.CompilationUnit;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.factory.JavaParserUtils;
import com.insidious.plugin.factory.testcase.ValueResourceContainer;
import com.insidious.plugin.pojo.ProjectTypeInfo;
import com.insidious.plugin.pojo.TestCaseUnit;
import com.insidious.plugin.pojo.TestSuite;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.FileContentUtil;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JUnitTestCaseWriter {
    private static final Logger logger = LoggerUtil.getInstance(JUnitTestCaseWriter.class);
    private final Project project;
    private final ObjectMapper objectMapper;
    private final ProjectTypeInfo projectTypeInfo = new ProjectTypeInfo();

    public JUnitTestCaseWriter(Project project, ObjectMapper objectMapper) {
        this.project = project;
        this.objectMapper = objectMapper;
    }

    public VirtualFile saveTestSuite(TestSuite testSuite) throws IOException {
        for (TestCaseUnit testCaseScript : testSuite.getTestCaseScripts()) {
            String basePath = fetchPathToSaveTestCase(testCaseScript);
            if (basePath == null) {
                basePath = project.getBasePath();
            }
            logger.info("[TEST CASE SAVE] basepath : " + basePath);
            ValueResourceContainer valueResourceContainer = testCaseScript.getTestGenerationState()
                    .getValueResourceMap();
            if (valueResourceContainer.getValueResourceMap().size() > 0) {
                String testResourcesDirPath =
                        basePath + "/src/test/resources/unlogged-fixtures/" + testCaseScript.getClassName();
                File resourcesDirFile = new File(testResourcesDirPath);
                resourcesDirFile.mkdirs();
                String resourceJson = objectMapper.writeValueAsString(valueResourceContainer.getValueResourceMap());

                String testResourcesFilePath =
                        testResourcesDirPath + "/" + valueResourceContainer.getResourceFileName() + ".json";
                try (FileOutputStream resourceFile = new FileOutputStream(testResourcesFilePath)) {
                    resourceFile.write(resourceJson.getBytes(StandardCharsets.UTF_8));
                }
                VirtualFileManager.getInstance().refreshAndFindFileByUrl(FileSystems.getDefault()
                        .getPath(testResourcesFilePath).toUri().toString());

                if (testCaseScript.getTestGenerationState().isSetupNeedsJsonResources()) {

                    String setupJsonFilePath = testResourcesDirPath + "/" + "setup" + ".json";
                    try (FileOutputStream resourceFile = new FileOutputStream(setupJsonFilePath)) {
                        resourceFile.write(resourceJson.getBytes(StandardCharsets.UTF_8));
                    }
                    VirtualFileManager.getInstance()
                            .refreshAndFindFileByUrl(FileSystems.getDefault()
                                    .getPath(setupJsonFilePath).toUri().toString());
                }
            }


            String testOutputDirPath = getTestDirectory(testCaseScript.getPackageName(), basePath);

            File outputDir = new File(testOutputDirPath);
            outputDir.mkdirs();
            File testcaseFile = new File(testOutputDirPath + "/" + testCaseScript.getClassName() + ".java");
            logger.info("[TEST CASE SAVE] testcaseFile : " + testcaseFile.getAbsolutePath());

            if (!testcaseFile.exists()) {
                try (FileOutputStream out = new FileOutputStream(testcaseFile)) {
                    out.write(testCaseScript.getCode()
                            .getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    InsidiousNotification.notifyMessage(
                            "Failed to write test case: " + testCaseScript + " -> "
                                    + e.getMessage(), NotificationType.ERROR);
                }


            } else {
                JavaParser javaParser = new JavaParser(new ParserConfiguration());
                ParseResult<CompilationUnit> parsedFile = javaParser.parse(
                        testcaseFile);
                if (!parsedFile.getResult().isPresent() || !parsedFile.isSuccessful()) {
                    InsidiousNotification.notifyMessage("<html>Failed to parse existing test case in the file, unable" +
                            " to" +
                            " add new test case. <br/>" + parsedFile.getProblems() + "</html>", NotificationType.ERROR);
                    return null;
                }
                CompilationUnit existingCompilationUnit = parsedFile.getResult().get();
//                ClassOrInterfaceDeclaration classDeclaration = existingCompilationUnit.getClassByName(
//                                testCaseScript.getClassName()).get();

//                TypeSpec newTestSpec = testCaseScript.getTestClassSpec();

                ParseResult<CompilationUnit> parseResult = javaParser.parse(testCaseScript.getCode());
                if (!parseResult.isSuccessful()) {
                    logger.error("Failed to parse test case to be written: \n" + testCaseScript.getCode() +
                            "\nProblems");
                    List<Problem> problems = parseResult.getProblems();
                    for (int i = 0; i < problems.size(); i++) {
                        Problem problem = problems.get(i);
                        logger.error("Problem [" + i + "] => " + problem);
                    }

                    InsidiousNotification.notifyMessage("Failed to parse test case to write " +
                            parseResult.getProblems(), NotificationType.ERROR
                    );
                    return null;
                }
                CompilationUnit newCompilationUnit = parseResult.getResult().get();

//                MethodDeclaration newMethodDeclaration =
//                        newCompilationUnit.getClassByName(testCaseScript.getClassName())
//                                .get().getMethodsByName(newTestSpec.methodSpecs.get(1).name).get(0);

                JavaParserUtils.mergeCompilationUnits(existingCompilationUnit, newCompilationUnit);

                try (FileOutputStream out = new FileOutputStream(testcaseFile)) {
                    out.write(existingCompilationUnit.toString()
                            .getBytes(StandardCharsets.UTF_8));
                } catch (Exception e) {
                    InsidiousNotification.notifyMessage(
                            "Failed to write test case: " + testCaseScript + " -> "
                                    + e.getMessage(), NotificationType.ERROR
                    );
                }


            }
            try {
                ensureTestUtilClass(basePath);
            } catch (Exception e) {
                logger.info("[ERROR] Failed to save UnloggedUtils to correct spot.");
            }

            @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                    .refreshAndFindFileByUrl(FileSystems.getDefault()
                            .getPath(testcaseFile.getAbsolutePath())
                            .toUri()
                            .toString());
            if (newFile == null) {
                return null;
            }
            newFile.refresh(true, false);


            List<VirtualFile> newFile1 = new ArrayList<>();
            newFile1.add(newFile);
            FileContentUtil.reparseFiles(project, newFile1, true);
            @Nullable Document newDocument = FileDocumentManager.getInstance().getDocument(newFile);

            FileEditorManager.getInstance(project).openFile(newFile, true, true);

            logger.info("Test case generated in [" + testCaseScript.getClassName() + "]\n" + testCaseScript);
            return newFile;
        }

        return null;
    }

    public String fetchPathToSaveTestCase(TestCaseUnit testCaseScript) {
        StringBuilder sb = new StringBuilder(testCaseScript.getClassName()
                .replaceFirst("Test", ""));
        sb.deleteCharAt(sb.length() - 1);

        PsiFile[] classBase = FilenameIndex.getFilesByName(project, sb + ".java",
                GlobalSearchScope.projectScope(project));

        if (classBase.length > 0) {
            if (classBase.length > 1) {
                //compare packages
                for (int i = 0; i < classBase.length; i++) {
                    PsiFile file = classBase[i];
                    if (file instanceof PsiJavaFile) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                        String packageName = psiJavaFile.getPackageName();
                        if (testCaseScript.getPackageName()
                                .equals(packageName)) {
                            return getBasePathForVirtualFile(classBase[i].getVirtualFile());
                        }
                    }
                }
                return getBasePathForVirtualFile(classBase[0].getVirtualFile());
            }
            return getBasePathForVirtualFile(classBase[0].getVirtualFile());
        }
        return null;
    }


    public void ensureTestUtilClass(String basePath) throws IOException {
        String testOutputDirPath;
        if (basePath != null) {
            testOutputDirPath = basePath + "src/test/java/io/unlogged";
        } else {
            basePath = project.getBasePath();
            testOutputDirPath = project.getBasePath() + "/src/test/java/io/unlogged";
        }
        if (basePath.charAt(basePath.length() - 1) == '/') {
            basePath = new StringBuilder(basePath).
                    deleteCharAt(basePath.length() - 1)
                    .toString();
        }
        File dirPath = new File(testOutputDirPath);
        if (!dirPath.exists()) {
            dirPath.mkdirs();
        }
        logger.info("[TEST CASE SAVE] UnloggedUtils path : " + basePath);
        // yikes right
        try {
            String oldFolderPath = basePath + "/src/test/java/io.unlogged";
            String oldFilePath = basePath + "/src/test/java/io.unlogged/UnloggedTestUtils.java";
            File oldFolder = FileSystems.getDefault()
                    .getPath(oldFolderPath)
                    .toFile();
            File oldUtilFile = FileSystems.getDefault()
                    .getPath(oldFilePath)
                    .toFile();
            if (oldUtilFile.exists()) {
                @Nullable VirtualFile oldFileInstance = VirtualFileManager.getInstance()
                        .refreshAndFindFileByUrl(FileSystems.getDefault()
                                .getPath(oldUtilFile.getAbsolutePath())
                                .toUri()
                                .toString());
                oldUtilFile.delete();
                oldFolder.delete();
                if (oldFileInstance != null) {
                    oldFileInstance.refresh(true, false);
                }
                @Nullable VirtualFile oldFolderInstance = VirtualFileManager.getInstance()
                        .refreshAndFindFileByUrl(FileSystems.getDefault()
                                .getPath(oldFolder.getAbsolutePath())
                                .toUri()
                                .toString());
                if (oldFolderInstance != null) {
                    oldFolderInstance.refresh(true, false);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Failed to delete the old version of the file: " + e.getMessage());
            // this is absolutely almost a mess
        }

        String utilFilePath = testOutputDirPath + "/UnloggedTestUtils.java";
        File utilFile = new File(utilFilePath);
        if (utilFile.exists()) {
            String utilFileContents = IOUtils.toString(new FileInputStream(utilFile), StandardCharsets.UTF_8);
            Pattern versionCaptureRegex = Pattern.compile("UnloggedTestUtils.Version: V([0-9]+)");
            Matcher versionMatcher = versionCaptureRegex.matcher(utilFileContents);
            if (versionMatcher.find()) {
                int version = Integer.parseInt(versionMatcher.group(1));
                if (version == 1) {
                    return;
                }
            }
            // util file already exist
            utilFile.delete();
        }

        String jacksonDatabindVersion = projectTypeInfo.getJacksonDatabindVersion();


        try (FileOutputStream writer = new FileOutputStream(utilFilePath)) {
            InputStream testUtilClassCode = this.getClass()
                    .getClassLoader()
                    .getResourceAsStream("code/jackson/UnloggedTestUtil.java");

            if (projectTypeInfo.getUsesGson()) {
                if (jacksonDatabindVersion == null)
                    testUtilClassCode = this.getClass()
                            .getClassLoader()
                            .getResourceAsStream("code/gson/UnloggedTestUtil.java");

            }

            assert testUtilClassCode != null;
            IOUtils.copy(testUtilClassCode, writer);
        }
        @Nullable VirtualFile newFile = VirtualFileManager.getInstance()
                .refreshAndFindFileByUrl(FileSystems.getDefault().getPath(utilFile.getAbsolutePath()).toUri().toString());

        if (newFile == null) {
            InsidiousNotification.notifyMessage("UnloggedTestUtil file was not created: "
                    + utilFile.getAbsolutePath(), NotificationType.WARNING);
            return;
        }

        newFile.refresh(true, false);

    }

    public String getTestDirectory(String packageName, String basePath) {
        if (!basePath.endsWith(File.separator)) {
            basePath = basePath + File.separator;
        }

        return basePath + "src" + File.separator + "test" + File.separator + "java"
                + File.separator + packageName.replaceAll("\\.", File.separator);
    }

    public String getTestResourcesDirectory(String basePath) {
        if (!basePath.endsWith(File.separator)) {
            basePath = basePath + File.separator;
        }

        return basePath + "src" + File.separator + "test" + File.separator + "resources" + File.separator;
    }

    public String getBasePathForVirtualFile(VirtualFile classFound) {
        String path = classFound.getPath();
        int last_index = path.lastIndexOf("src");
        if (last_index == -1) {
            return null;
        }
        return path.substring(0, last_index);
    }


}
