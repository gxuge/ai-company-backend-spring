package org.jeecg.modules.openapi.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * MiniMax 演示接口防线配置。
 */
@NoArgsConstructor
@Data
@Component
@ConfigurationProperties(prefix = MiniMaxDemoGuardConfigBean.PREFIX)
public class MiniMaxDemoGuardConfigBean {
    public static final String PREFIX = "jeecg.airag.minimax.demo.guard";

    /**
     * 是否开启防线。
     */
    private boolean enabled = true;

    /**
     * 访问令牌。
     */
    private String accessToken;

    /**
     * 请求头名称。
     */
    private String headerName = "X-AI-ACCESS-TOKEN";

    /**
     * 单接口单IP每分钟最大请求数。
     */
    private int maxRequestsPerMinute = 30;

    /**
     * 对话最大字符数。
     */
    private int maxChatChars = 4000;

    /**
     * 语音最大字符数。
     */
    private int maxTtsChars = 1000;

    /**
     * 文生图提示词最大字符数。
     */
    private int maxImagePromptChars = 1000;
}
