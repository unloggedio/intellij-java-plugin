package com.insidious.plugin.client.pojo;

import com.insidious.common.UploadFile;

public class MatchedUploadFile extends UploadFile {
    public MatchedUploadFile(String filePath, long threadId) {
        super(filePath, threadId, null, null);
    }
}
