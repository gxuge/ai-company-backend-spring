package org.jeecg.modules.system.dto.tschatsession;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TsChatMessageTtsDto {

    /** 会话 ID */
    @NotNull(message = "sessionId不能为空")
    private Long sessionId;

    /** 待播报消息 ID */
    @NotNull(message = "messageId不能为空")
    private Long messageId;

    /** 指定音色档案 ID，可为空 */
    private Long voiceProfileId;

    /** 指定音色 ID，可为空 */
    private String voiceId;

    /** 语速 */
    private Double speed;

    /** 音调 */
    private Double pitch;

    /** 音量 */
    private Double volume;

    /** 是否以 audio/mpeg 流式返回：true=流式，false=JSON */
    private Boolean stream;

    public void applyDefaults() {
        if (this.voiceId != null) {
            this.voiceId = this.voiceId.trim();
            if (this.voiceId.isEmpty()) {
                this.voiceId = null;
            }
        }
        if (this.stream == null) {
            this.stream = Boolean.FALSE;
        }
    }
}
