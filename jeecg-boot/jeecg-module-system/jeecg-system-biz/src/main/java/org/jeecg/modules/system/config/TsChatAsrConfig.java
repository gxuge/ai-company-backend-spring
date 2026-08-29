package org.jeecg.modules.system.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Chat speech recognition configuration.
 */
@Data
@Component
@ConfigurationProperties(prefix = "jeecg.ai.asr")
public class TsChatAsrConfig {

    private boolean enabled;
    private String provider = "dashscope";
    private String apiKey;
    private String workspaceId;
    private String region = "cn-beijing";
    private String model = "qwen-audio-3.0-asr-flash-streaming";
    private String format = "pcm";
    private int sampleRate = 16000;
    private String language = "zh";
    private int maxSentenceSilenceMs = 800;
    private long ticketTtlSeconds = 60;

    public String getWebsocketApiUrl() {
        if ("ap-southeast-1".equals(region)) {
            return "wss://" + workspaceId + ".ap-southeast-1.maas.aliyuncs.com/api-ws/v1/inference";
        }
        return "wss://" + workspaceId + ".cn-beijing.maas.aliyuncs.com/api-ws/v1/inference";
    }
}
