package org.jeecg.modules.openapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Prompt chat runtime config.
 *
 * Uses AIRAG database models instead of YAML provider/model credentials.
 */
@Data
@Component
@ConfigurationProperties(prefix = "jeecg.airag.prompt-chat")
public class PromptChatConfigBean {

    /**
     * AIRAG app id to resolve prompt chat model from (airag_app.model_id).
     */
    private String appId;

    /**
     * Optional explicit AIRAG model id. If set, this takes priority over appId.
     */
    private String modelId;

    /**
     * Prompt 文本模型默认是否禁用 think/reasoning 输出。
     */
    private Boolean noThinkDefault = true;

    /**
     * 工具调用不支持时是否自动降级为普通文本调用。
     */
    private Boolean toolCallAutoDowngrade = true;
}
