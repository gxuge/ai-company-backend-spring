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
 * Agent 消息表实体。
 *
 * @author codex
 * @date 2026/6/25
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_agent_chat_message")
public class TsAgentChatMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID。
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 会话ID。
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 会话内消息序号。
     */
    @TableField("message_no")
    private Long messageNo;

    /**
     * 消息角色：user/assistant/system/tool。
     */
    @TableField("role_type")
    private String roleType;

    /**
     * 发送方类型：user/main_agent/sub_agent/system/tool。
     */
    @TableField("sender_type")
    private String senderType;

    /**
     * Agent 编码。
     */
    @TableField("agent_code")
    private String agentCode;

    /**
     * 消息正文。
     */
    @TableField("content")
    private String content;

    /**
     * 原始内容。
     */
    @TableField("content_raw")
    private String contentRaw;

    /**
     * 结构化内容 JSON。
     */
    @TableField("content_json")
    private String contentJson;

    /**
     * 是否用户可见：0否 1是。
     */
    @TableField("visible_to_user")
    private Integer visibleToUser;

    /**
     * 内容格式：text/markdown/json。
     */
    @TableField("content_format")
    private String contentFormat;

    /**
     * 消息状态：streaming/success/failed。
     */
    @TableField("message_status")
    private String messageStatus;

    /**
     * 父消息ID。
     */
    @TableField("parent_message_id")
    private Long parentMessageId;

    /**
     * Agent运行ID。
     */
    @TableField("run_id")
    private String runId;

    /**
     * 提示词编码。
     */
    @TableField("prompt_code")
    private String promptCode;

    /**
     * 文本模型ID。
     */
    @TableField("model_id")
    private String modelId;

    /**
     * Token统计。
     */
    @TableField("token_usage_json")
    private String tokenUsageJson;

    /**
     * 扩展JSON。
     */
    @TableField("ext_json")
    private String extJson;

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
