package org.jeecg.modules.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * MiniMax 语音请求 DTO。
 */
@Data
public class MiniMaxTtsRequestDto {

    /**
     * 待转换文本。
     */
    @NotBlank(message = "text不能为空")
    private String text;

    /**
     * 音色ID。
     */
    @NotBlank(message = "voiceId不能为空")
    private String voiceId;
}
