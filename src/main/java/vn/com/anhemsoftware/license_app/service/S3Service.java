package vn.com.anhemsoftware.license_app.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

    /**
     * Upload file to S3 and return the public URL of the file.
     *
     * @param file     file uploaded from client
     * @param taskId   ID of the task (used as a prefix in the S3 key)
     * @return URL of the file on S3
     */
    String uploadTaskImage(MultipartFile file, Long taskId);

    /**
     * Delete file on S3 by URL.
     *
     * @param imageUrl Full URL of the file to be deleted
     */
    void deleteFile(String imageUrl);
}
