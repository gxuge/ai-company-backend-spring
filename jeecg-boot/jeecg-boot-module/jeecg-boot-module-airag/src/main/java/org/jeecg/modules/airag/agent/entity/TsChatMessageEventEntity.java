package org.jeecg.modules.airag.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 聊天消息事件表实体。
 *
 * @author codex
 * @date 2026/6/16
 */
@Data
@TableName("ts_chat_message_events")
public class TsChatMessageEventEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 事件ID。
     */
    @TableId(type = IdType.INPUT)
    private String id;
    /**
     * 消息ID。
     */
    @TableField("message_id")
    private String messageId;
    /**
     * 会话ID。
     */
    @TableField("session_id")
    private Long sessionId;
    /**
     * Agent会话记录ID。
     */
    @TableField("agent_session_id")
    private Long agentSessionId;
    /**
     * 运行ID。
     */
    @TableField("run_id")
    private String runId;
    /**
     * 事件块类型。
     */
    private String type;
    /**
     * 节点名称。
     */
    private String name;
    /**
     * 主要文本内容。
     */
    private String content;
    /**
     * 状态值。
     */
    private Integer status;
    /**
     * 扩展 JSON。
     */
    private String json;
    /**
     * 是否删除。
     */
    private Integer isDeleted;
    /**
     * 删除时间。
     */
    private Date deletedAt;
    /**
     * 创建时间。
     */
    private Date createdAt;
    /**
     * 更新时间。
     */
    private Date updatedAt;
}
