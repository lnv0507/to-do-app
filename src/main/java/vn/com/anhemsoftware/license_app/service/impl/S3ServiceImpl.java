package vn.com.anhemsoftware.license_app.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import vn.com.anhemsoftware.license_app.service.S3Service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3ServiceImpl implements S3Service {

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Override
    public String uploadTaskImage(MultipartFile file, Long taskId) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        // Key format: tasks/{taskId}/{uuid}{extension}
        String key = "tasks/" + taskId + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Unable to read uploaded file data", e);
        }

        return buildFileUrl(key);
    }

    @Override
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        // Extract key from URL: https://{bucket}.s3.{region}.amazonaws.com/{key}
        String prefix = "https://" + bucketName + ".s3." + region + ".amazonaws.com/";
        if (!imageUrl.startsWith(prefix)) {
            throw new IllegalArgumentException("URL does not belong to the current bucket: " + imageUrl);
    }
        String key = imageUrl.substring(prefix.length());

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException(
                    "Only image files are accepted (jpeg, png, gif, webp). Received content-type: " + file.getContentType()
            );
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size cannot exceed 5MB");
        }
    }

    private String buildFileUrl(String key) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
    }
}
