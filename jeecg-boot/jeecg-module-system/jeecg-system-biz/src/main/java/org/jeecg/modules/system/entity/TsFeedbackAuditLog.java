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
 * 反馈内容审核日志实体。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_feedback_audit_log")
public class TsFeedbackAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 审核日志主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 审核目标：feedback、comment、append。 */
    @TableField("target_type")
    private String targetType;

    /** 审核目标 ID。 */
    @TableField("target_id")
    private Long targetId;

    /** 所属反馈 ID。 */
    @TableField("feedback_id")
    private Long feedbackId;

    /** 审核前状态。 */
    @TableField("previous_status")
    private String previousStatus;

    /** 审核后状态：approved、rejected。 */
    @TableField("audit_status")
    private String auditStatus;

    /** 审核原因。 */
    @TableField("audit_reason")
    private String auditReason;

    /** 审核人 ID。 */
    @TableField("auditor_id")
    private String auditorId;

    /** 创建时间。 */
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
