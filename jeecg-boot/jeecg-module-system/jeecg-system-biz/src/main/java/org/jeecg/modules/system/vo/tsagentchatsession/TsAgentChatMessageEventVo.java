package org.jeecg.modules.system.vo.tsagentchatsession;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 会话消息事件前端展示对象。
 *
 * @author codex
 * @date 2026/7/15
 */
@Data
public class TsAgentChatMessageEventVo {

    /**
     * 事件ID。
     */
    private String id;

    /**
     * 事件类型：llm/tool。
     */
    private String type;

    /**
     * 模型或 Tool 名称。
     */
    private String name;

    /**
     * 实际执行节点名称。
     */
    private String nodeName;

    /**
     * 节点类型。
     */
    private String nodeType;

    /**
     * 事件结果摘要。
     */
    private String content;

    /**
     * 事件状态：1成功、0失败、2运行中或未知。
     */
    private Integer status;

    /**
     * 完整执行数据：input/output/error/metrics。
     */
    private Map<String, Object> data = new LinkedHashMap<>();

    /**
     * 创建时间。
     */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
