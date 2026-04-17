package org.jeecg.modules.system.dto.tschatsession;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TsChatAiReplyDto {

    /** 会话 ID */
    @NotNull(message = "sessionId不能为空")
    private Long sessionId;

    /** 用户当前输入的文本内容 */
    @NotBlank(message = "userContent不能为空")
    private String userContent;

    /** 带入上下文的历史消息条数，默认 12，最大 30 */
    private Integer historyCount;

    /** 指定音色档案 ID（可选，优先级低于 voiceId） */
    private Long voiceProfileId;

    /** 指定 MiniMax 音色 ID（可选，优先级最高） */
    private String voiceId;

    /** 是否生成语音，默认 true。false 时仅生成文本回复 */
    private Boolean generateVoice;

    /** 语速（建议范围 0.8~1.2） */
    private Double speed;

    /** 音调（建议范围 -6~6） */
    private Double pitch;

    /** 音量（建议范围 0.8~1.2） */
    private Double volume;

    /** 规范化默认参数，避免历史窗口过大或非法 */
    public void applyDefaults() {
        if (this.historyCount == null || this.historyCount <= 0) {
            this.historyCount = 12;
        } else if (this.historyCount > 30) {
            this.historyCount = 30;
        }
        if (this.generateVoice == null) {
            this.generateVoice = Boolean.TRUE;
        }
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
