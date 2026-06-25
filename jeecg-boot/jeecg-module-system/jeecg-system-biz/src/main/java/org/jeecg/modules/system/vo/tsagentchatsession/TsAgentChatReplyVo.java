package org.jeecg.modules.system.vo.tsagentchatsession;

import lombok.Data;

import java.util.Date;

/**
 * Agent 回复结果。
 *
 * @author codex
 * @date 2026/6/25
 */
@Data
public class TsAgentChatReplyVo {

    /**
     * 会话ID。
     */
    private Long sessionId;

    /**
     * 用户消息ID。
     */
    private Long userMessageId;

    /**
     * 助手消息ID。
     */
    private Long assistantMessageId;

    /**
     * 回复文本。
     */
    private String contentText;

    /**
     * 提示词编码。
     */
    private String promptCode;

    /**
     * 提示词版本。
     */
    private String promptVersion;

    /**
     * 渲染后的提示词。
     */
    private String renderedPrompt;

    /**
     * 创建时间。
     */
    private Date createdAt;
}
