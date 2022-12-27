package com.insidious.plugin.upload.minio;

import com.insidious.plugin.upload.zip.ZipFiles;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReportIssue {
    public ReportIssue() {
    }

    public Task.Backgroundable zippingAndUploadTask(Project project, String sessionObjectKey, String ideaLogObjectKey) {
        Task.Backgroundable zippingTask = new Task.Backgroundable(project, "Unlogged", false) {
            String seLogDirPath;
            String zipFileName;
            String pathPrefix;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ZipFiles zipFiles = new ZipFiles();
                seLogDirPath = getLatestSeLogFolderPath();
                int lastIndexOf = seLogDirPath.lastIndexOf("/");
                pathPrefix = seLogDirPath.substring(0, lastIndexOf);
                zipFileName = seLogDirPath.substring(lastIndexOf + 1) + ".zip";

                checkProgressIndicator("Zipping session logs to upload", null);
                File file = new File(pathPrefix + "/" + zipFileName);
//                if (!file.exists()) {
                zipFiles.zipDirectory(new File(seLogDirPath), pathPrefix + "/" + zipFileName);
//                }
                String intellijFullVersion = ApplicationInfo.getInstance().getFullVersion();
                String ideaLogFilePath = System.getProperty("user.home") + "/Library/Logs/JetBrains/IntelliJIdea" + intellijFullVersion + "/idea.log";
                File ideaLogFile = new File(ideaLogFilePath);

                // for intellij community
                if (!ideaLogFile.exists()) {
                    ideaLogFilePath = System.getProperty("user.home") + "/Library/Logs/JetBrains/IdealC" + intellijFullVersion + "/idea.log";
                    ideaLogFile = new File(ideaLogFilePath);
                }

                // for ubuntu
                if (!ideaLogFile.exists()) {
                    ideaLogFilePath = System.getProperty("user.home") + "/.cache/JetBrains/IdealC" + intellijFullVersion + "/log/idea.log";
                    ideaLogFile = new File(ideaLogFilePath);
                }

                checkProgressIndicator("Uploading session logs", null);

                FileUploader fileUploader = new FileUploader();
                try {
                    fileUploader.uploadFile(sessionObjectKey, pathPrefix + "/" + zipFileName);
                    fileUploader.uploadFile(ideaLogObjectKey, ideaLogFile.getAbsolutePath());

                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                } catch (NoSuchAlgorithmException ex) {
                    throw new RuntimeException(ex);
                } catch (InvalidKeyException ex) {
                    throw new RuntimeException(ex);
                }
            }
        };

        return zippingTask;
    }

    public String getLatestSeLogFolderPath() {
        String parentFolder = System.getProperty("user.home") + "/.videobug/sessions/";
        File sessionDirectory = new File(parentFolder);

        if (sessionDirectory.listFiles() == null) {
            return null;
        }

        List<File> sessionFolders = Arrays.stream(Objects.requireNonNull(sessionDirectory.listFiles()))
                .sorted((a, b) -> -1 * a.getName()
                        .compareTo(b.getName()))
                .filter(e -> e.isDirectory() && e.getName()
                        .startsWith("selogger-output-"))
                .collect(Collectors.toList());

        File latestSessionDir = sessionFolders.get(0);

        return latestSessionDir.getAbsolutePath();
    }

    private void checkProgressIndicator(String text1, String text2) {
        if (ProgressIndicatorProvider.getGlobalProgressIndicator() != null) {
            if (ProgressIndicatorProvider.getGlobalProgressIndicator()
                    .isCanceled()) {
                throw new ProcessCanceledException();
            }
            if (text2 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText2(text2);
            }
            if (text1 != null) {
                ProgressIndicatorProvider.getGlobalProgressIndicator()
                        .setText(text1);
            }
        }
    }
}
