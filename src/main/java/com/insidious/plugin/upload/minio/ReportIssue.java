package com.insidious.plugin.upload.minio;

import com.insidious.plugin.extension.InsidiousNotification;
import com.insidious.plugin.upload.zip.ZipFiles;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
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
    private final static Logger logger = LoggerUtil.getInstance(ReportIssue.class);

    public ReportIssue() {
    }

    public Task.Backgroundable zippingAndUploadTask(Project project, String sessionObjectKey) {
        Task.Backgroundable zippingTask = new Task.Backgroundable(project, "Unlogged", false) {
            String seLogDirPath;
            String zipFileName;
            String pathPrefix;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                ZipFiles zipFiles = new ZipFiles();
                seLogDirPath = getLatestSeLogFolderPath();
                if (!seLogDirPath.equals("")) {
                    int lastIndexOf = seLogDirPath.lastIndexOf(File.separator);
                    pathPrefix = seLogDirPath.substring(0, lastIndexOf);
                    zipFileName = seLogDirPath.substring(lastIndexOf + 1) + ".zip";
                } else {
                    pathPrefix = System.getProperty("user.home") + File.separator + ".videobug" + File.separator + "sessions";
                    zipFileName = "empty-selogger-folder.zip";
                }

                checkProgressIndicator("Zipping session logs to upload", null);

                try {
                    zipFiles.zipDirectory(seLogDirPath, pathPrefix + File.separator + zipFileName);
                    System.out.println("created zip file to upload at " + pathPrefix + File.separator + zipFileName);
                } catch (Exception e) {
                    InsidiousNotification.notifyMessage("Failed to submit bug report. Please try again!\n" +
                            "or <a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.", NotificationType.ERROR);
                    logger.warn(e.getMessage(), e);
                    return;
                }

                checkProgressIndicator("Uploading bug logs and idea.log", null);
                FileUploader fileUploader = new FileUploader();
                try {
                    fileUploader.uploadFile(sessionObjectKey, pathPrefix + File.separator + zipFileName);
                    System.out.println("uploaded zip file at" + pathPrefix + File.separator + zipFileName);
                } catch (IOException | NoSuchAlgorithmException | InvalidKeyException ex) {
                    InsidiousNotification.notifyMessage("Failed to upload bug report. Try again!\n" +
                            "or <a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>", NotificationType.ERROR);
                    logger.warn(ex.getMessage(), ex);
                    ex.printStackTrace();
                }

                checkProgressIndicator("Issue Upload completed!", null);
            }
        };

        return zippingTask;
    }

    public String getLatestSeLogFolderPath() {
        String parentFolder = System.getProperty("user.home") + File.separator + ".videobug" + File.separator + "sessions" + File.separator;
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

        File latestSessionDir = null;
        if (sessionFolders.size() > 0) {
            latestSessionDir = sessionFolders.get(0);
            return latestSessionDir.getAbsolutePath();
        }

        return "";
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
