package org.jeecg.modules.system.dto.tsagentchatsession;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Agent 会话更新参数。
 *
 * @author codex
 * @date 2026/6/26
 */
@Data
public class TsAgentChatSessionUpdateDto {

    /**
     * 会话ID。
     */
    @NotNull(message = "id不能为空")
    private Long id;

    /**
     * 会话标题。
     */
    private String sessionTitle;

    /**
     * 会话摘要。
     */
    private String sessionSummary;

    /**
     * 会话记忆快照。
     */
    private String memoryJson;

    /**
     * 会话结构化状态。
     */
    private String stateJson;
}
