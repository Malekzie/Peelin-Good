package com.sait.peelin.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.UUID;

@Service
public class ProfilePhotoStorageService {

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
                        .build();

                s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            }

            return baseUrl.replaceAll("/+$", "") + "/" + key;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload profile photo");
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
}
