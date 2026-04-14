package com.sait.peelin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

@Service
public class ProfilePhotoStorageService {
    private static final Logger log = LoggerFactory.getLogger(ProfilePhotoStorageService.class);

    private final String endpoint;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private final String baseUrl;
    private final String prefix;

    public ProfilePhotoStorageService(
            @Value("${spring.spaces.endpoint:}") String endpoint,
            @Value("${spring.spaces.bucket:}") String bucket,
            @Value("${spring.spaces.access-key:}") String accessKey,
            @Value("${spring.spaces.secret-key:}") String secretKey,
            @Value("${spring.spaces.base_url:}") String baseUrl,
            @Value("${spring.spaces.prefix:customers}") String prefix
    ) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.baseUrl = baseUrl;
        this.prefix = prefix;
    }

    /**
     * Uploads a file to an arbitrary folder within the configured bucket.
     * The object key is {@code folder/entityId/<uuid>.<ext>}.
     *
     * @param folder   top-level folder name (e.g. "customers", "employees", "bakery")
     * @param entityId identifier used as the sub-folder (UUID or numeric ID as string)
     * @param file     multipart file to upload
     * @return publicly accessible URL of the uploaded object
     */
    public String uploadToFolder(String folder, String entityId, MultipartFile file) {
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(bucket)
                || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)
                || !StringUtils.hasText(baseUrl)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Object storage is not configured");
        }
        try {
            String ext = extensionFor(file.getContentType());
            String key = String.format("%s/%s/%s%s", cleanPrefix(folder), entityId, UUID.randomUUID(), ext);

            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            String endpointUrl = endpoint.startsWith("http") ? endpoint : "https://" + endpoint;
            try (S3Client s3 = S3Client.builder()
                    .endpointOverride(URI.create(endpointUrl))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(false)
                    .build()) {

                PutObjectRequest req = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build();

                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            }

            return baseUrl.replaceAll("/+$", "") + "/" + key;
        } catch (Exception e) {
            log.error("Upload failed for entity {} in folder {} via endpoint {}", entityId, folder, endpoint, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload file: " + e.getMessage());
        }
    }

    /**
     * Uploads a product image directly into the {@code products/} folder with no entity sub-folder.
     * The object key is {@code products/<uuid>.<ext>}. If the product already has an image, the old
     * object is deleted from storage after the new one is successfully uploaded.
     *
     * @param file        multipart image file to upload
     * @param existingUrl current image URL on the product record, or {@code null} if none
     * @return publicly accessible URL of the newly uploaded object
     */
    public String uploadProductImage(MultipartFile file, String existingUrl) {
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(bucket)
                || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)
                || !StringUtils.hasText(baseUrl)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Object storage is not configured");
        }
        try {
            String ext = extensionFor(file.getContentType());
            String key = String.format("products/%s%s", UUID.randomUUID(), ext);

            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            String endpointUrl = endpoint.startsWith("http") ? endpoint : "https://" + endpoint;
            try (S3Client s3 = S3Client.builder()
                    .endpointOverride(URI.create(endpointUrl))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(false)
                    .build()) {

                PutObjectRequest req = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build();

                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

                // Delete the old image only after the new one is successfully uploaded
                if (StringUtils.hasText(existingUrl)) {
                    String oldKey = extractObjectKey(existingUrl);
                    if (StringUtils.hasText(oldKey)) {
                        try {
                            s3.deleteObject(DeleteObjectRequest.builder()
                                    .bucket(bucket)
                                    .key(oldKey)
                                    .build());
                        } catch (Exception deleteEx) {
                            log.warn("Failed to delete old product image {} from bucket {}", oldKey, bucket, deleteEx);
                        }
                    }
                }
            }

            return baseUrl.replaceAll("/+$", "") + "/" + key;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Product image upload failed via endpoint {}", endpoint, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload product image: " + e.getMessage());
        }
    }

    public String uploadCustomerProfilePhoto(UUID userId, MultipartFile file) {
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(bucket)
                || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)
                || !StringUtils.hasText(baseUrl)) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Object storage is not configured");
        }
        try {
            String ext = extensionFor(file.getContentType());
            String key = String.format("%s/%s/%s%s", cleanPrefix(prefix), userId, UUID.randomUUID(), ext);

            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            String endpointUrl = endpoint.startsWith("http") ? endpoint : "https://" + endpoint;
            try (S3Client s3 = S3Client.builder()
                    .endpointOverride(URI.create(endpointUrl))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(false)
                    .build()) {

                PutObjectRequest req = PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(file.getContentType())
                        // Required so mobile clients can fetch the image URL directly.
                        .acl(ObjectCannedACL.PUBLIC_READ)
                        .build();

                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            }

            return baseUrl.replaceAll("/+$", "") + "/" + key;
        } catch (Exception e) {
            log.error("Profile photo upload failed for user {} to bucket {} via endpoint {}",
                    userId, bucket, endpoint, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to upload profile photo: " + e.getMessage());
        }
    }

    public void deleteCustomerProfilePhoto(String photoUrl) {
        if (!StringUtils.hasText(photoUrl)) return;
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(bucket)
                || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            log.warn("Skipping profile photo delete because object storage is not configured");
            return;
        }
        String key = extractObjectKey(photoUrl);
        if (!StringUtils.hasText(key)) {
            log.warn("Skipping profile photo delete because key could not be extracted from URL: {}", photoUrl);
            return;
        }
        try {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
            String endpointUrl = endpoint.startsWith("http") ? endpoint : "https://" + endpoint;
            try (S3Client s3 = S3Client.builder()
                    .endpointOverride(URI.create(endpointUrl))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                    .forcePathStyle(false)
                    .build()) {
                s3.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
            }
        } catch (Exception e) {
            log.warn("Failed to delete profile photo object {} from bucket {}", key, bucket, e);
        }
    }

    private static String extensionFor(String contentType) {
        if (contentType == null) return ".jpg";
        String ct = contentType.toLowerCase();
        if (ct.contains("png")) return ".png";
        return ".jpg";
    }

    private static String cleanPrefix(String raw) {
        if (!StringUtils.hasText(raw)) return "customers";
        return raw.replaceAll("^/+", "").replaceAll("/+$", "");
    }

    private static String extractObjectKey(String photoUrl) {
        try {
            URI uri = URI.create(photoUrl.trim());
            String path = uri.getPath();
            if (!StringUtils.hasText(path)) return null;
            return path.replaceFirst("^/+", "");
        } catch (Exception e) {
            return null;
        }
    }
}
