package com.insidious.plugin.upload.minio;

import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class FileUploader {

    final static String ENDPOINT = "https://videobug-plugin-issue-selogs.s3.ap-south-1.amazonaws.com";
    final static String ACCESS_KEY = "AKIASLJNAK3E7SJDTVUW";
    final static String SECRET_KEY = "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";
    final static String BUCKET_NAME = "videobug-plugin-issue-selogs";
    final static String BUCKET_REGION = "ap-south-1";

    public String uploadFile(String objectKey, String pathToFile)
            throws IOException, NoSuchAlgorithmException, InvalidKeyException, RuntimeException {
        try {
            // Create a minioClient with the MinIO server playground, its access key and secret key.
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(ENDPOINT)
                            .credentials(ACCESS_KEY, SECRET_KEY)
                            .region(BUCKET_REGION)
                            .build();

            ObjectWriteResponse response = null;

            response = minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(objectKey)
                            .filename(pathToFile)
                            .build());

            System.out.println(
                    pathToFile + " is successfully uploaded as "
                            + "object '" + objectKey + "' to bucket '" + BUCKET_NAME + "'");

            return ENDPOINT + "/" + (response == null ? objectKey : response.object());
        } catch (MinioException e) {
            throw new RuntimeException(e);
        }
    }
}