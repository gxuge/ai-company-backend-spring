package org.jeecg.modules.system.dto.tsagentchatsession;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Agent 会话保存参数。
 *
 * @author codex
 * @date 2026/6/25
 */
@Data
public class TsAgentChatSessionSaveDto {

    /**
     * 应用ID。
     */
    @NotBlank(message = "appId不能为空")
    private String appId;

    /**
     * 主 Agent 编码。
     */
    private String agentCode;

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
