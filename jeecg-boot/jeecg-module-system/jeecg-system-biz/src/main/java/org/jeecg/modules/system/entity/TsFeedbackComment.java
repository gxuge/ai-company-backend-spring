package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

/**
 * 反馈评论与二级回复实体。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_feedback_comment")
public class TsFeedbackComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 评论主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属反馈 ID。 */
    @TableField("feedback_id")
    private Long feedbackId;

    /** 评论用户 ID。 */
    @TableField("user_id")
    private String userId;

    /** 一级评论为空，二级回复保存一级评论 ID。 */
    @TableField("parent_id")
    private Long parentId;

    /** 被回复用户 ID。 */
    @TableField("reply_to_user_id")
    private String replyToUserId;

    /** 评论内容。 */
    private String content;

    /** 点赞数量。 */
    @TableField("like_count")
    private Integer likeCount;

    /** 是否官方回复：0 否，1 是。 */
    @TableField("is_official")
    private Integer isOfficial;

    /** 审核状态：pending、approved、rejected。 */
    @TableField("audit_status")
    private String auditStatus;

    /** 审核驳回原因。 */
    @TableField("audit_reason")
    private String auditReason;

    /** 审核人 ID。 */
    @TableField("audited_by")
    private String auditedBy;

    /** 审核时间。 */
    @TableField("audited_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditedAt;

    /** 逻辑删除：0 正常，1 已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间。 */
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
