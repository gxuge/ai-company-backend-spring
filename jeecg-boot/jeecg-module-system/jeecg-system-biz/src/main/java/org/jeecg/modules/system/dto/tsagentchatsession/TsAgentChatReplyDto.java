package org.jeecg.modules.system.dto.tsagentchatsession;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Agent 回复请求参数。
 *
 * @author codex
 * @date 2026/6/25
 */
@Data
public class TsAgentChatReplyDto {

    /**
     * 会话ID。
     */
    @NotNull(message = "sessionId不能为空")
    private Long sessionId;

    /**
     * 用户输入内容。
     */
    @NotBlank(message = "userInput不能为空")
    private String userInput;

    /**
     * 历史消息条数。
     */
    private Integer historyCount;

    /**
     * 规范化默认参数。
     */
    public void applyDefaults() {
        if (historyCount == null || historyCount <= 0) {
            historyCount = 12;
        } else if (historyCount > 30) {
            historyCount = 30;
        }
    }
}
