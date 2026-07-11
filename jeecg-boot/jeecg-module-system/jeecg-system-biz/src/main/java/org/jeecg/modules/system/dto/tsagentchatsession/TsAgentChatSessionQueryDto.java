package org.jeecg.modules.system.dto.tsagentchatsession;

import lombok.Data;

/**
 * Agent 会话分页查询参数。
 *
 * @author codex
 * @date 2026/6/25
 */
@Data
public class TsAgentChatSessionQueryDto {

    /**
     * 页码。
     */
    private Integer pageNo;

    /**
     * 每页条数。
     */
    private Integer pageSize;

    /**
     * 关键词。
     */
    private String keyword;

    /**
     * 会话状态。
     */
    private String sessionStatus;

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
