package org.jeecg.modules.airag.agent.runtime;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 会话历史消息。
 *
 * <p>承载模型组装消息所需的角色、正文和消息归属，避免运行时上下文依赖业务消息实体。</p>
 */
@Data
@NoArgsConstructor
public class AgentConversationMessage {
    /**
     * 业务消息标识。
     */
    private String messageId;
    /**
     * 触发该回复的用户消息标识。
     */
    private String parentMessageId;
    /**
     * 消息角色：user/assistant。
     */
    private String role;
    /**
     * 消息正文。
     */
    private String content;
    /**
     * 生成该消息的 Agent 编码。
     */
    private String agentCode;
    /**
     * 生成该消息的节点名称。
     */
    private String sourceNodeName;

    /**
     * 构造不带消息归属的兼容历史消息。
     */
    public AgentConversationMessage(String messageId, String role, String content) {
        this(messageId, null, role, content, null, null);
    }

    /**
     * 构造包含消息归属的历史消息。
     */
    public AgentConversationMessage(String messageId,
                                    String parentMessageId,
                                    String role,
                                    String content,
                                    String agentCode,
                                    String sourceNodeName) {
        this.messageId = messageId;
        this.parentMessageId = parentMessageId;
        this.role = role;
        this.content = content;
        this.agentCode = agentCode;
        this.sourceNodeName = sourceNodeName;
    }
}
