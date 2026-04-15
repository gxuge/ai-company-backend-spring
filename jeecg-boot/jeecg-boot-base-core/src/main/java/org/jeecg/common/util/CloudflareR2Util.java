package org.jeecg.common.util;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.constant.SymbolConstant;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.util.filter.SsrfFileTypeFilter;
import org.jeecg.common.util.filter.StrAttackFilter;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

/**
 * Cloudflare R2 upload utility (S3-compatible, using Minio client).
 */
@Slf4j
public class CloudflareR2Util {

    private static String endpoint;
    private static String accessKey;
    private static String secretKey;
    private static String bucketName;
    private static String publicDomain;

    private static MinioClient minioClient;

    private CloudflareR2Util() {
    }

    public static void setEndpoint(String endpoint) {
        CloudflareR2Util.endpoint = normalizeEndpoint(endpoint);
        minioClient = null;
    }

    public static void setAccessKey(String accessKey) {
        CloudflareR2Util.accessKey = accessKey;
        minioClient = null;
    }

    public static void setSecretKey(String secretKey) {
        CloudflareR2Util.secretKey = secretKey;
        minioClient = null;
    }

    public static void setBucketName(String bucketName) {
        CloudflareR2Util.bucketName = bucketName;
    }

    public static void setPublicDomain(String publicDomain) {
        CloudflareR2Util.publicDomain = normalizeDomain(publicDomain);
    }

    /**
     * Upload MultipartFile with default bucket.
     */
    public static String upload(MultipartFile file, String bizPath) throws Exception {
        return upload(file, bizPath, null);
    }

    /**
     * Upload MultipartFile with optional custom bucket.
     */
    public static String upload(MultipartFile file, String bizPath, String customBucket) throws Exception {
        // Sanitize bizPath to avoid path traversal.
        bizPath = StrAttackFilter.filter(bizPath);
        // Validate file type for upload security.
        SsrfFileTypeFilter.checkUploadFileType(file, bizPath);

        String orgName = file.getOriginalFilename();
        if ("".equals(orgName)) {
            orgName = file.getName();
        }
        orgName = CommonUtils.getFileName(orgName);
        String objectName = bizPath + "/"
                + (orgName.indexOf(".") == -1
                ? orgName + "_" + System.currentTimeMillis()
                : orgName.substring(0, orgName.lastIndexOf(".")) + "_" + System.currentTimeMillis() + orgName.substring(orgName.lastIndexOf(".")));

        if (objectName.startsWith(SymbolConstant.SINGLE_SLASH)) {
            objectName = objectName.substring(1);
        }
        objectName = objectName.replace("\\", "/");

        try (InputStream stream = file.getInputStream()) {
            return uploadInternal(stream, objectName, resolveBucket(customBucket));
        }
    }

    /**
     * Upload InputStream with default bucket.
     */
    public static String upload(InputStream stream, String relativePath) throws Exception {
        String objectName = (relativePath == null ? "" : relativePath).trim();
        if (objectName.startsWith(SymbolConstant.SINGLE_SLASH)) {
            objectName = objectName.substring(1);
        }
        objectName = objectName.replace("\\", "/");
        return uploadInternal(stream, objectName, resolveBucket(null));
    }

    private static String uploadInternal(InputStream stream, String objectName, String uploadBucket) throws Exception {
        MinioClient client = initClient();

        PutObjectArgs objectArgs = PutObjectArgs.builder()
                .bucket(uploadBucket)
                .object(objectName)
                .contentType("application/octet-stream")
                .stream(stream, -1, 10 * 1024 * 1024)
                .build();
        client.putObject(objectArgs);
        return buildPublicUrl(uploadBucket, objectName);
    }

    private static synchronized MinioClient initClient() {
        if (minioClient == null) {
            if (oConvertUtils.isEmpty(endpoint) || oConvertUtils.isEmpty(accessKey) || oConvertUtils.isEmpty(secretKey)) {
                throw new JeecgBootException("Cloudflare R2 config is incomplete (endpoint/accessKey/secretKey)");
            }
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
        }
        return minioClient;
    }

    private static String resolveBucket(String customBucket) {
        String value = oConvertUtils.isNotEmpty(customBucket) ? customBucket : bucketName;
        if (oConvertUtils.isEmpty(value)) {
            throw new JeecgBootException("Cloudflare R2 bucketName is empty");
        }
        return value;
    }

    private static String buildPublicUrl(String uploadBucket, String objectName) {
        String cleanObjectName = objectName == null ? "" : objectName.replace("\\", "/");
        if (oConvertUtils.isNotEmpty(publicDomain)) {
            return publicDomain + SymbolConstant.SINGLE_SLASH + cleanObjectName;
        }
        return endpoint + SymbolConstant.SINGLE_SLASH + uploadBucket + SymbolConstant.SINGLE_SLASH + cleanObjectName;
    }

    private static String normalizeEndpoint(String raw) {
        if (oConvertUtils.isEmpty(raw)) {
            return raw;
        }
        String value = raw.trim();
        if (!value.startsWith(CommonConstant.STR_HTTP)) {
            value = "https://" + value;
        }
        while (value.endsWith(SymbolConstant.SINGLE_SLASH)) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String normalizeDomain(String raw) {
        if (oConvertUtils.isEmpty(raw)) {
            return raw;
        }
        String value = raw.trim();
        while (value.endsWith(SymbolConstant.SINGLE_SLASH)) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }
}
