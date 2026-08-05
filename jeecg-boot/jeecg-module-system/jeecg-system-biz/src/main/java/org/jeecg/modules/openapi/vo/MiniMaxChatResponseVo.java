package org.jeecg.modules.openapi.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

/**
 * MiniMax 对话响应 VO。
 */
@Data
public class MiniMaxChatResponseVo {

    /**
     * 对话返回内容。
     */
    private String content;

    @JsonIgnore
    private String provider;
    @JsonIgnore
    private String modelName;
    @JsonIgnore
    private Integer inputTokens;
    @JsonIgnore
    private Integer outputTokens;
    @JsonIgnore
    private Integer totalTokens;
    @JsonIgnore
    private String usageRawJson;
    @JsonIgnore
    private Long durationMs;
}
