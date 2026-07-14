package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * Agent 会话表实体。
 *
 * @author codex
 * @date 2026/6/25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_agent_chat_session")
public class TsAgentChatSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话编号。
     */
    @TableField("session_no")
    private String sessionNo;

    /**
     * 应用ID。
     */
    @TableField("app_id")
    private String appId;

    /**
     * 主 Agent 编码。
     */
    @TableField("agent_code")
    private String agentCode;

    /**
     * 当前接管会话的 Agent 编码。
     */
    @TableField("active_agent_code")
    private String activeAgentCode;

    /**
     * 当前 Agent 最后切换时间。
     */
    @TableField("active_agent_updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date activeAgentUpdatedAt;

    /**
     * 下一轮恢复执行的节点名称。
     */
    @TableField("active_node_name")
    private String activeNodeName;

    /**
     * 当前子 Agent 流程阶段。
     */
    @TableField("active_stage")
    private String activeStage;

    /**
     * 当前子 Agent 可恢复流程状态。
     */
    @TableField("agent_flow_state_json")
    private String agentFlowStateJson;

    /**
     * 用户ID。
     */
    @TableField("user_id")
    private String userId;

    /**
     * 会话标题。
     */
    @TableField("session_title")
    private String sessionTitle;

    /**
     * 会话摘要。
     */
    @TableField("session_summary")
    private String sessionSummary;

    /**
     * 会话状态：active/archived/deleted。
     */
    @TableField("session_status")
    private String sessionStatus;

    /**
     * 会话记忆快照。
     */
    @TableField("memory_json")
    private String memoryJson;

    /**
     * 会话结构化状态。
     */
    @TableField("state_json")
    private String stateJson;

    /**
     * 子 Agent 最近执行历史快照。
     */
    @TableField("sub_agent_history_json")
    private String subAgentHistoryJson;

    /**
     * 最后一条消息ID。
     */
    @TableField("last_message_id")
    private Long lastMessageId;

    /**
     * 最后一条消息时间。
     */
    @TableField("last_message_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastMessageAt;

    /**
     * 消息总数。
     */
    @TableField("message_count")
    private Integer messageCount;

    /**
     * 轮次数。
     */
    @TableField("turn_count")
    private Integer turnCount;

    /**
     * 是否删除：0否 1是。
     */
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 创建时间。
     */
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /**
     * 更新时间。
     */
    @TableField("updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
