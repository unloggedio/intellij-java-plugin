package com.insidious.plugin.upload.minio;

import com.insidious.plugin.Constants;
import com.insidious.plugin.InsidiousNotification;
import com.insidious.plugin.upload.zip.ZipFiles;
import com.insidious.plugin.util.LoggerUtil;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicatorProvider;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import net.openhft.chronicle.core.util.Time;


import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReportIssue {
    static final String NO_SELOG_FOLDER_NAME = "empty-selogger-folder";
    static final String SERVER_REPO_MAIL_ADDRESS = "contact-project+insidious1-server-32379664-issue-@incoming.gitlab.com";
    private final static Logger logger = LoggerUtil.getInstance(ReportIssue.class);
    String issueTitle;
    String formattedDescription;
    private String sessionKeyObject;


    public ReportIssue() {
    }

    public Task.Backgroundable zippingAndUploadTask(Project project, String userEmail, String issueTitle, String description, String checkboxLabels) {
        prepareForUpload(userEmail, issueTitle, description, checkboxLabels);
        String objectKey = this.sessionKeyObject;

        Task.Backgroundable zippingTask = new Task.Backgroundable(project, "Unlogged", false) {
            String seLogDirPath;
            String zipFileName;
            String pathPrefix;

            @Override
            public void run( ProgressIndicator indicator) {
                ZipFiles zipFiles = new ZipFiles();
                seLogDirPath = getLatestSeLogFolderPath();
                if (!seLogDirPath.equals("")) {
                    int lastIndexOf = seLogDirPath.lastIndexOf(File.separator);
                    pathPrefix = seLogDirPath.substring(0, lastIndexOf);
                    zipFileName = seLogDirPath.substring(lastIndexOf + 1) + ".zip";
                } else {
                    pathPrefix = Constants.HOME_PATH + File.separator + "sessions";
                    zipFileName = "empty-selogger-folder.zip";
                }

                checkProgressIndicator("Zipping session logs to upload", null);

                try {
                    zipFiles.zipDirectory(seLogDirPath, pathPrefix + File.separator + zipFileName);
                    System.out.println("created zip file to upload at " + pathPrefix + File.separator + zipFileName);
                } catch (Exception e) {
                    InsidiousNotification.notifyMessage("Failed to zip bug report. Please try again!\n" +
                                    "or <a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>.",
                            NotificationType.ERROR);
                    logger.warn(e.getMessage(), e);
                    return;
                }

                // create the email text and S3 bucket URI
                checkProgressIndicator("Uploading bug logs and idea.log", null);
                FileUploader fileUploader = new FileUploader();
                try {
                    fileUploader.uploadFile(objectKey, pathPrefix + File.separator + zipFileName);
                    logger.warn("uploaded zip file at" + pathPrefix + File.separator + zipFileName);
                } catch (IOException | NoSuchAlgorithmException | InvalidKeyException ex) {
                    InsidiousNotification.notifyMessage(
                            "Failed to upload bug report.\n Check your internet connection! \n " +
                                    "or <a href=\"https://discord.gg/274F2jCrxp\">Reach out to us</a>",
                            NotificationType.ERROR);
                    logger.warn(ex.getMessage(), ex);
                    ex.printStackTrace();
                    return;
                }

                checkProgressIndicator("Issue Upload completed!", null);

                createMailAndRedirectUser();
            }
        };

        return zippingTask;
    }

    public Task.Backgroundable sendSupportMessage(Project project, String userEmail, String issueName, String customMessage) {
        prepareForMail(userEmail, issueName, customMessage);
        Task.Backgroundable sendingMailTask = new Task.Backgroundable(project, "Unlogged", false) {
            @Override
            public void run(ProgressIndicator indicator) {
                checkProgressIndicator("Sending mail", null);
                createMailAndRedirectUser();
            }
        };
        return sendingMailTask;
    }

    public void prepareForUpload(String userEmail, String issueTitle, String description, String checkBoxLabels) {

        File selogDir = new File(getLatestSeLogFolderPath());

        String dirName = !selogDir.getName().equals("") ? selogDir.getName() : NO_SELOG_FOLDER_NAME;
        String s3BucketParentPath = userEmail + "/" + dirName + "-" + Time.uniqueId();
        this.sessionKeyObject = s3BucketParentPath + "/" + dirName + ".zip";

        System.out.println(this.sessionKeyObject);

        String sessionURI = FileUploader.ENDPOINT + "/" + FileUploader.BUCKET_NAME + "/" + this.sessionKeyObject;

        String issueDescription = "Issue Raised by: `" + userEmail + "`\n\n"
                + (dirName.equals(
                NO_SELOG_FOLDER_NAME) ? "session folder was empty, session zip only contains idea.log! \n\n" : "")
                + checkBoxLabels.toString()
                + "\n"
                + "[Session Logs](" + sessionURI.replace("+", "%2B").replace("@", "%40") + ")"
                + "\n\n"
                + description;

        this.formattedDescription = issueDescription;
        this.issueTitle = issueTitle;
    }

    public void prepareForUpload(String userEmail, String issueTitle, String customMessage) {
        File selogDir = new File(getLatestSeLogFolderPath());
        String dirName = !selogDir.getName().equals("") ? selogDir.getName() : NO_SELOG_FOLDER_NAME;
        String s3BucketParentPath = userEmail + "/" + dirName + "-" + Time.uniqueId();
        this.sessionKeyObject = s3BucketParentPath + "/" + dirName + ".zip";
        System.out.println(this.sessionKeyObject);
        String sessionURI = FileUploader.ENDPOINT + "/" + FileUploader.BUCKET_NAME + "/" + this.sessionKeyObject;
        String issueDescription = "Issue Raised by: `" + userEmail + "`\n\n"
                + (dirName.equals(NO_SELOG_FOLDER_NAME) ? "The session folder was empty. \n\n" : "")
                + "\n"
                + "[Session Logs](" + sessionURI.replace("+", "%2B").replace("@", "%40") + ")"
                + "\n\n"
                + "Issue description : \n\n"
                + customMessage;
        this.formattedDescription = issueDescription;
        this.issueTitle = issueTitle + " faced by "+userEmail;
        }

    public void prepareForMail(String userEmail, String issueTitle, String customMessage) {
        String issueDescription = "Issue Raised by: `" + userEmail + "`\n\n"
                + "Issue description : \n"
                + customMessage;
        this.formattedDescription = issueDescription;
        if(issueTitle.equals("Other"))
        {
            issueTitle = "Issue";
        }
        this.issueTitle = issueTitle + " faced by "+userEmail;
    }

    private void createMailAndRedirectUser() {

        Desktop desktop = Desktop.getDesktop();
        String gitlabMail = SERVER_REPO_MAIL_ADDRESS;

        String mailFromBrowser = "https://mail.google.com/mail/?view=cm&fs=1&to=" + URLEncoder.encode(gitlabMail,
                StandardCharsets.UTF_8).replace("+", "%20")
                + "&su=" + URLEncoder.encode(issueTitle, StandardCharsets.UTF_8).replace("+", "%20")
                + "&body=" + URLEncoder.encode(formattedDescription, StandardCharsets.UTF_8).replace("+", "%20");

        URI uri = URI.create(mailFromBrowser);

        try {
            desktop.browse(uri);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public String getLatestSeLogFolderPath() {
        String parentFolder = Constants.HOME_PATH + File.separator + "sessions" + File.separator;
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
