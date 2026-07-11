package org.jeecg.modules.airag.kb.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * KB embedding配置。
 */
@NoArgsConstructor
@Data
@Component
@ConfigurationProperties(prefix = KbEmbeddingProperties.PREFIX)
public class KbEmbeddingProperties {
    /**
     * 配置前缀。
     */
    public static final String PREFIX = "jeecg.kb.embedding";

    /**
     * 模型提供方。
     */
    private String provider;

    /**
     * 模型名称。
     */
    private String modelName;

    /**
     * 模型BaseUrl。
     */
    private String baseUrl;

    /**
     * API Key。
     */
    private String apiKey;

    /**
     * Secret Key。
     */
    private String secretKey;

    /**
     * 最大输入字符数，超过后会截断。
     */
    private Integer maxTextLength = 12000;

    /**
     * 批处理大小。
     */
    private Integer batchSize = 16;

    /**
     * 默认回退维度。
     */
    private Integer fallbackDimension = 256;

    /**
     * processing超时分钟数，超过后允许重置为pending。
     */
    private Integer processingTimeoutMinutes = 30;
}
