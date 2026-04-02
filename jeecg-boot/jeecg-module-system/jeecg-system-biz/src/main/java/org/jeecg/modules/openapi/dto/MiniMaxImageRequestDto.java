package org.jeecg.modules.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * MiniMax 文生图请求 DTO。
 */
@Data
public class MiniMaxImageRequestDto {

    /**
     * 绘图提示词。
     */
    @NotBlank(message = "prompt不能为空")
    private String prompt;
}
