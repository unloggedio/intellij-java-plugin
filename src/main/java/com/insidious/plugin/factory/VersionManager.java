package com.insidious.plugin.factory;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

public class VersionManager {

    final static Logger log = LoggerUtil.getInstance(VersionManager.class);
    private String version;

    private Properties versionProperties = new Properties();

    private String gitLastTag;
    private String gitHash;
    private String gitBranchName;
    private String gitIsCleanTag;

    public String getVersion() {
        return version;
    }

    public String getGitLastTag() {
        return gitLastTag;
    }

    public String getGitHash() {
        return gitHash;
    }

    public String getGitBranchName() {
        return gitBranchName;
    }

    public String getGitIsCleanTag() {
        return gitIsCleanTag;
    }

    VersionManager() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("classpath:/version.properties");

        if (inputStream == null) {
            ResourceBundle versionBundle = ResourceBundle.getBundle("version");

            if (versionBundle.containsKey("gitLastTag")) {
                gitLastTag = versionBundle.getString("gitLastTag");
            } else {
                gitLastTag = "last-tag-not-found";
            }

            if (versionBundle.containsKey("gitHash")) {
                gitHash = versionBundle.getString("gitHash");
            } else {
                gitHash = "git-hash-not-found";
            }


            if (versionBundle.containsKey("gitBranchName")) {
                gitBranchName = versionBundle.getString("gitHash");
            } else {
                gitBranchName = "git-branch-name-not-found";
            }


            if (versionBundle.containsKey("gitIsCleanTag")) {
                gitIsCleanTag = versionBundle.getString("gitIsCleanTag");
            } else {
                gitIsCleanTag = "git-isCleanTag-not-found";
            }


            if (versionBundle.containsKey("version")) {
                version = versionBundle.getString("version");
            } else {
                version = "git-version-not-found";
            }

        }

        if (inputStream == null) {
            // When running unit tests, no jar is built, so we load a copy of the file that we saved during build.gradle.
            // Possibly this also is the case during debugging, therefore we save in bin/main instead of bin/test.
            try {
                inputStream = new FileInputStream("bin/main/version.properties");
                versionProperties.load(inputStream);
                gitLastTag = versionProperties.getProperty("gitLastTag", "last-tag-not-found");
                gitHash = versionProperties.getProperty("gitHash", "git-hash-not-found");
                gitBranchName = versionProperties.getProperty("gitBranchName", "git-branch-name-not-found");
                gitIsCleanTag = versionProperties.getProperty("gitIsCleanTag", "git-isCleanTag-not-found");
                version = versionProperties.getProperty("version", "git-version-not-found");

            } catch (IOException e) {
                log.warn("failed to load verion.properties file: " + e.getMessage());
            }
        }


    }
}