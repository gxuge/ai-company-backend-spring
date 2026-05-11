package org.jeecg.modules.openapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模板渲染后文本生成配置。
 * 仅用于“Prompt 拼接后”的文本调用链路。
 */
@Data
@Component
@ConfigurationProperties(prefix = "jeecg.airag.prompt-chat")
public class PromptChatConfigBean {
    /**
     * 文本提供商：deepseek|qwen|minimax
     */
    private String provider = "qwen";

    private final Qwen qwen = new Qwen();
    private final DeepSeek deepseek = new DeepSeek();

    @Data
    public static class Qwen {
        /**
         * DashScope API Key。
         */
        private String apiKey;
        /**
         * DashScope 兼容模式基地址。
         */
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode";
        /**
         * 模型名。
         */
        private String model = "qwen-max";
        /**
         * 采样温度。
         */
        private Double temperature = 0.7D;
        /**
         * 连接超时毫秒。
         */
        private Integer connectTimeoutMs = 3000;
        /**
         * 读取超时毫秒。
         */
        private Integer readTimeoutMs = 30000;
        /**
         * 重试次数。
         */
        private Integer retryMaxAttempts = 2;
        /**
         * 重试间隔毫秒。
         */
        private Integer retryBackoffMs = 300;
    }

    @Data
    public static class DeepSeek {
        /**
         * DeepSeek API Key。
         */
        private String apiKey;
        /**
         * DeepSeek OpenAI 兼容基地址。
         */
        private String baseUrl = "https://api.deepseek.com";
        /**
         * 模型名（V4 推荐：deepseek-v4-pro 或 deepseek-v4-flash）。
         */
        private String model = "deepseek-v4-pro";
        /**
         * 采样温度。
         */
        private Double temperature = 0.7D;
        /**
         * 推理强度（可选）：low|medium|high。
         */
        private String reasoningEffort;
        /**
         * 思考模式（可选）：enabled|disabled。
         */
        private String thinkingType;
        /**
         * 连接超时毫秒。
         */
        private Integer connectTimeoutMs = 3000;
        /**
         * 读取超时毫秒。
         */
        private Integer readTimeoutMs = 30000;
        /**
         * 重试次数。
         */
        private Integer retryMaxAttempts = 2;
        /**
         * 重试间隔毫秒。
         */
        private Integer retryBackoffMs = 300;
    }
}