package org.jeecg.modules.system.dto.tsvoiceprofile;

import lombok.Data;

@Data
public class TsVoiceProfilePreviewDto {
    /** 音色档案 ID（可选，优先级低于 voiceId） */
    private Long voiceProfileId;
    /** 直传 MiniMax voiceId（可选，优先级最高） */
    private String voiceId;
    /** 试听文案 */
    private String previewText;
    /** 语速（建议范围 0.8~1.2） */
    private Double speed;
    /** 音调（建议范围 -6~6） */
    private Double pitch;
    /** 音量（建议范围 0.8~1.2） */
    private Double volume;

    public void normalize() {
        this.voiceId = trimToNull(this.voiceId);
        this.previewText = trimToNull(this.previewText);
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
