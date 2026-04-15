package org.jeecg.config.oss;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.util.CloudflareR2Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * Cloudflare R2 配置（S3 兼容）。
 */
@Lazy(false)
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "jeecg", name = "uploadType", havingValue = CommonConstant.UPLOAD_TYPE_R2)
public class CloudflareR2Config {

    @Value("${jeecg.cloudflareR2.endpoint:}")
    private String endpoint;

    @Value("${jeecg.cloudflareR2.accessKey:}")
    private String accessKey;

    @Value("${jeecg.cloudflareR2.secretKey:}")
    private String secretKey;

    @Value("${jeecg.cloudflareR2.bucketName:}")
    private String bucketName;

    @Value("${jeecg.cloudflareR2.publicDomain:}")
    private String publicDomain;

    @PostConstruct
    public void initCloudflareR2Configuration() {
        CloudflareR2Util.setEndpoint(endpoint);
        CloudflareR2Util.setAccessKey(accessKey);
        CloudflareR2Util.setSecretKey(secretKey);
        CloudflareR2Util.setBucketName(bucketName);
        CloudflareR2Util.setPublicDomain(publicDomain);
        log.info("Cloudflare R2 uploader initialized, bucket={}", bucketName);
    }
}
