package org.jeecg.modules.system.dto.tsagentchatsession;

import lombok.Data;

/**
 * Agent 消息事件分页查询参数。
 *
 * @author codex
 * @date 2026/7/14
 */
@Data
public class TsAgentChatMessageEventQueryDto {

    /**
     * 会话ID。
     */
    private Long sessionId;

    /**
     * 触发当前 Run 的用户消息ID。
     */
    private Long messageId;

    /**
     * 事件类型：subagent/tool。
     */
    private String type;

    /**
     * SubAgent 或 Tool 名称。
     */
    private String name;

    /**
     * 实际执行节点名称。
     */
    private String nodeName;

    /**
     * 事件状态：1成功、0失败、2运行中或未知。
     */
    private Integer status;

    /**
     * 页码。
     */
    private Integer pageNo;

    /**
     * 每页条数。
     */
    private Integer pageSize;

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
