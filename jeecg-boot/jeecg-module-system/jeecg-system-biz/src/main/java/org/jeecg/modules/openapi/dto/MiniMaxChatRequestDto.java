package org.jeecg.modules.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * MiniMax 对话请求 DTO。
 */
@Data
public class MiniMaxChatRequestDto {

    /**
     * 对话提示词。
     */
    @NotBlank(message = "prompt不能为空")
    private String prompt;
}
