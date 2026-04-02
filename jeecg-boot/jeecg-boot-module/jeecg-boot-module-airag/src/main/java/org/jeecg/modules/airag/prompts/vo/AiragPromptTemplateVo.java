package org.jeecg.modules.airag.prompts.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * Classpath 提示词模板对象
 *
 * @author chenrui
 * @date 2026/3/31
 */
@Data
@Schema(description = "AIRAG classpath prompt 模板")
public class AiragPromptTemplateVo {

    @Schema(description = "模板编码")
    private String code;

    @Schema(description = "模板版本")
    private String version;

    @Schema(description = "模板内容分段，key 为 section 名称")
    private Map<String, String> sections;
}
