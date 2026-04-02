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

    /** 规范化默认参数，避免历史窗口过大或非法 */
    public void applyDefaults() {
        if (this.historyCount == null || this.historyCount <= 0) {
            this.historyCount = 12;
            return;
        }
        if (this.historyCount > 30) {
            this.historyCount = 30;
        }
    }
}
