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

    /** 语速（建议范围 0.8 ~ 1.2） */
    private Double speed;

    /** 音调（建议范围 -6 ~ 6） */
    private Double pitch;

    /** 音量（建议范围 0.8 ~ 1.2） */
    private Double volume;

    public void normalize() {
        this.text = trimToNull(this.text);
        this.voiceId = trimToNull(this.voiceId);
        this.speed = clamp(this.speed, 0.8D, 1.2D);
        this.pitch = clamp(this.pitch, -6D, 6D);
        this.volume = clamp(this.volume, 0.8D, 1.2D);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static Double clamp(Double value, double min, double max) {
        if (value == null) {
            return null;
        }
        return Math.max(min, Math.min(max, value));
    }
}
