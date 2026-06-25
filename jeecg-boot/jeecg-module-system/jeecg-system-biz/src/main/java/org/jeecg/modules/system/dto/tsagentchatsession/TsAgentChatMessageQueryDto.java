package org.jeecg.modules.system.dto.tsagentchatsession;

import lombok.Data;

/**
 * Agent 消息分页查询参数。
 *
 * @author codex
 * @date 2026/6/25
 */
@Data
public class TsAgentChatMessageQueryDto {

    /**
     * 会话ID。
     */
    private Long sessionId;

    /**
     * 页码。
     */
    private Integer pageNo;

    /**
     * 每页条数。
     */
    private Integer pageSize;

    /**
     * 消息角色。
     */
    private String roleType;

    /**
     * 消息状态。
     */
    private String messageStatus;

    /**
     * 关键词。
     */
    private String keyword;

    /**
     * 规范化分页参数。
     */
    public void applyDefaults() {
        if (pageNo == null || pageNo <= 0) {
            pageNo = 1;
        }
        if (pageSize == null || pageSize <= 0) {
            pageSize = 20;
        }
        if (pageSize > 100) {
            pageSize = 100;
        }
    }
}
