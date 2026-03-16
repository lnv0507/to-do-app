package vn.com.anhemsoftware.license_app.service;

import org.springframework.web.multipart.MultipartFile;

public interface S3Service {

    /**
     * Upload file lên S3 và trả về URL public của file.
     *
     * @param file     file upload từ client
     * @param taskId   ID của task (dùng làm prefix trong S3 key)
     * @return URL của file trên S3
     */
    String uploadTaskImage(MultipartFile file, Long taskId);

    /**
     * Xóa file trên S3 theo URL.
     *
     * @param imageUrl URL đầy đủ của file cần xóa
     */
    void deleteFile(String imageUrl);
}
