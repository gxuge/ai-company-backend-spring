package org.jeecg.modules.system.dto.tschatsession;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TsChatTemplateReplyDto {

    /** 会话 ID */
    @NotNull(message = "sessionId不能为空")
    private Long sessionId;

    /** 用户当前输入 */
    @NotBlank(message = "userInput不能为空")
    private String userInput;

    /** 当前轮发言角色 ID，可为空，默认回退到 session.targetRoleId */
    private Long activeRoleId;

    /** 带入上下文的历史消息条数，默认 12，最大 30 */
    private Integer historyCount;

    /** 指定当前轮参考的 AI 消息 ID（可选） */
    private Long lastAssistantMessageId;

    public void applyDefaults() {
        if (this.historyCount == null || this.historyCount <= 0) {
            this.historyCount = 12;
        } else if (this.historyCount > 30) {
            this.historyCount = 30;
        }
        if (this.userInput != null) {
            this.userInput = this.userInput.trim();
            if (this.userInput.isEmpty()) {
                this.userInput = null;
            }
        }
    }
}
