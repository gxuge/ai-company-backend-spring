package org.jeecg.modules.openapi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.util.StringUtils;

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

    /**
     * 参考图地址，可选；传入后走图生图能力。
     */
    private String referenceImageUrl;

    /**
     * 是否将生成结果上传到自有存储。
     */
    private Boolean uploadGeneratedMedia;

    public void normalize() {
        this.prompt = trimToNull(this.prompt);
        this.referenceImageUrl = trimToNull(this.referenceImageUrl);
        if (this.uploadGeneratedMedia == null) {
            this.uploadGeneratedMedia = Boolean.FALSE;
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }
}
