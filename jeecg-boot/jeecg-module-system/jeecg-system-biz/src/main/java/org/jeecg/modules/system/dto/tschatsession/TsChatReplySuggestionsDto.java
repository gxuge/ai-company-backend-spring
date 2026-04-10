package org.jeecg.modules.system.dto.tschatsession;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TsChatReplySuggestionsDto {

    /** 会话 ID */
    @NotNull(message = "sessionId不能为空")
    private Long sessionId;

    /** 带入上下文的历史消息条数，默认 12，最大 30 */
    private Integer historyCount;

    /** 用户当前输入框草稿（可选） */
    private String userDraft;

    /** 指定当前轮参考的 AI 消息 ID（可选） */
    private Long lastAssistantMessageId;

    /** 规范化默认参数，避免历史窗口过大或非法 */
    public void applyDefaults() {
        if (this.historyCount == null || this.historyCount <= 0) {
            this.historyCount = 12;
        } else if (this.historyCount > 30) {
            this.historyCount = 30;
        }
        if (this.userDraft != null) {
            this.userDraft = this.userDraft.trim();
            if (this.userDraft.isEmpty()) {
                this.userDraft = null;
            }
        }
    }
}
