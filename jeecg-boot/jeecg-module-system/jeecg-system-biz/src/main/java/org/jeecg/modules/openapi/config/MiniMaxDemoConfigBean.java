package org.jeecg.modules.openapi.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MiniMax demo config.
 */
@NoArgsConstructor
@Data
@Component
@ConfigurationProperties(prefix = MiniMaxDemoConfigBean.PREFIX)
public class MiniMaxDemoConfigBean {
    public static final String PREFIX = "jeecg.airag.minimax";

    /**
     * OpenAI-compatible key for MiniMax API.
     */
    private String apiKey;

    /**
     * MiniMax API root url.
     */
    private String apiBaseUrl = "https://api.minimax.io";

    /**
     * TTS model.
     */
    private String ttsModel = "speech-2.8-hd";

    /**
     * Image generation model.
     */
    private String imageModel = "image-01";

    /**
     * Image aspect ratio.
     */
    private String imageAspectRatio = "16:9";

    /**
     * HTTP connect timeout in milliseconds.
     */
    private int connectTimeoutMs = 3000;

    /**
     * HTTP read timeout in milliseconds.
     */
    private int readTimeoutMs = 30000;

    /**
     * Max retry attempts for MiniMax API calls.
     */
    private int retryMaxAttempts = 2;

    /**
     * Retry backoff interval in milliseconds.
     */
    private int retryBackoffMs = 300;

    /**
     * Whether generated media should be uploaded to the configured object storage.
     */
    private boolean uploadGeneratedMedia = true;

    /**
     * Business path for generated images.
     */
    private String imageUploadBizPath = "airag/minimax/image";

    /**
     * Business path for generated audio files.
     */
    private String audioUploadBizPath = "airag/minimax/audio";
}
