package com.insidious.plugin.upload.zip;

import com.intellij.idea.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipFiles {
    List<String> filesListInDir = new ArrayList<String>();

    /**
     * This method zips the directory
     *
     * @param dirPath
     * @param zipFileName
     */
    public void zipDirectory(String dirPath, String zipFileName) throws IOException {
        File dir = null;
        if (!dirPath.equals("")) {
            dir = new File(dirPath);
        }

        try (FileOutputStream fos = new FileOutputStream(zipFileName);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            // adding the selogger folder files
            if (!dirPath.equals(""))
                populateFilesList(dir);

            // adding the idea.log file also in the zip file to upload
            Path ideaLogFilePath = LoggerFactory.getLogFilePath();
            filesListInDir.add(ideaLogFilePath.toString());

            //now zip files one by one
            //create ZipOutputStream to write to the zip file
            for (String filePath : filesListInDir) {
                File fileTobeZipped = new File(filePath);
                //for ZipEntry we need to keep only relative file path, so we used substring on absolute path
                ZipEntry ze = new ZipEntry(fileTobeZipped.getName());
                zos.putNextEntry(ze);
                try (FileInputStream fis = new FileInputStream(filePath)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, len);
                    }
                } catch (IOException e) {
                    throw new IOException("Failed to Zip selogger directory!");
                }
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new IOException("Failed to Zip selogger directory!");
        }
    }

    /**
     * This method populates all the files in a directory to a List
     *
     * @param dir
     * @throws IOException
     */
    private void populateFilesList(File dir) throws IOException {
        if (dir.exists())
            return;
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile()) {
                if (!file.getName().endsWith(".dat"))
                    filesListInDir.add(file.getAbsolutePath());
            } else {
                if (!file.getName().equals("cache")) {
                    populateFilesList(file);
                }
            }
        }
    }
}