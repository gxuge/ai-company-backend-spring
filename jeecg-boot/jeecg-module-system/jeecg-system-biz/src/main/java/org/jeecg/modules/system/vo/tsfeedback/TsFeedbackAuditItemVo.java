package org.jeecg.modules.system.vo.tsfeedback;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 管理端反馈内容审核项。
 */
@Data
public class TsFeedbackAuditItemVo {

    /** 审核目标：feedback、comment、append。 */
    private String targetType;

    /** 审核目标 ID。 */
    private Long targetId;

    /** 所属反馈 ID。 */
    private Long feedbackId;

    /** 发布用户 ID。 */
    private String userId;

    /** 发布用户名称。 */
    private String userName;

    /** 反馈标题，仅反馈目标有值。 */
    private String title;

    /** 待审核内容。 */
    private String content;

    /** 一级评论 ID，仅二级回复有值。 */
    private Long parentId;

    /** 是否官方回复，仅评论目标有值。 */
    private Boolean official;

    /** 审核状态：pending、approved、rejected。 */
    private String auditStatus;

    /** 审核原因。 */
    private String auditReason;

    /** 审核人 ID。 */
    private String auditedBy;

    /** 审核人名称。 */
    private String auditorName;

    /** 审核时间。 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditedAt;

    /** 内容创建时间。 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
