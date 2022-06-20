package com.insidious.plugin.factory;

import com.insidious.plugin.util.LoggerUtil;
import com.intellij.openapi.diagnostic.Logger;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class VersionManager {

    final static Logger log = LoggerUtil.getInstance(VersionManager.class);

    private Properties versionProperties = new Properties();

    private String gitLastTag;
    private String gitHash;
    private String gitBranchName;
    private String gitIsCleanTag;

    public String getVersion() {
        return versionProperties.getProperty("version");
    }

    VersionManager() {
        String AllGitVersionProperties = "";
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("classpath:/version.properties");

        if (inputStream == null) {
            // When running unit tests, no jar is built, so we load a copy of the file that we saved during build.gradle.
            // Possibly this also is the case during debugging, therefore we save in bin/main instead of bin/test.
            try {
                inputStream = new FileInputStream("bin/main/version.properties");
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        try {
            versionProperties.load(inputStream);
        } catch (IOException e) {
            AllGitVersionProperties += e.getMessage() + ":";
            log.error("Could not load classpath:/version.properties", e);
        }

        gitLastTag = versionProperties.getProperty("gitLastTag", "last-tag-not-found");
        gitHash = versionProperties.getProperty("gitHash", "git-hash-not-found");
        gitBranchName = versionProperties.getProperty("gitBranchName", "git-branch-name-not-found");
        gitIsCleanTag = versionProperties.getProperty("gitIsCleanTag", "git-isCleanTag-not-found");

        Set<Map.Entry<Object, Object>> mainPropertiesSet = versionProperties.entrySet();
        for (Map.Entry oneEntry : mainPropertiesSet) {
            AllGitVersionProperties += "+" + oneEntry.getKey() + ":" + oneEntry.getValue();
        }
    }
}