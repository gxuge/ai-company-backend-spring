package org.jeecg.modules.airag.safety.moderation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 意图复审使用的最近上下文消息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationContextMessage {
    /**
     * 消息角色：user/assistant。
     */
    private String role;
    /**
     * 消息正文。
     */
    private String content;
}
