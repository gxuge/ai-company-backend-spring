package org.jeecg.modules.openapi.vo;

import lombok.Data;

/**
 * Prompt 渲染后的分段结果。
 */
@Data
public class PromptRenderedSectionsVo {
    private String templatePath;
    private String code;
    private String version;
    private String developerPrompt;
    private String userPrompt;
    private String outputSchemaHint;
    private String toolSchema;
    private String renderedPrompt;
}

