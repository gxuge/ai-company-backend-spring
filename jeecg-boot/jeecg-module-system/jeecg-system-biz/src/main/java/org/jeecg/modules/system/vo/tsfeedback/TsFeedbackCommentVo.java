package org.jeecg.modules.system.vo.tsfeedback;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 反馈评论与回复展示对象。
 */
@Data
public class TsFeedbackCommentVo {

    /** 评论 ID。 */
    private Long id;

    /** 所属反馈 ID。 */
    private Long feedbackId;

    /** 评论用户 ID。 */
    private String userId;

    /** 评论用户名称。 */
    private String userName;

    /** 评论用户头像。 */
    private String userAvatar;

    /** 一级评论为空，二级回复保存一级评论 ID。 */
    private Long parentId;

    /** 被回复用户 ID。 */
    private String replyToUserId;

    /** 被回复用户名称。 */
    private String replyToUserName;

    /** 评论内容。 */
    private String content;

    /** 点赞数量。 */
    private Integer likeCount;

    /** 当前用户是否已点赞。 */
    private Boolean liked;

    /** 是否官方回复。 */
    private Boolean official;

    /** 审核状态：pending、approved、rejected。 */
    private String auditStatus;

    /** 审核驳回原因。 */
    private String auditReason;

    /** 审核时间。 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditedAt;

    /** 当前一级评论的二级回复总数。 */
    private Integer replyCount;

    /** 当前一级评论预览回复，默认最多 2 条。 */
    private List<TsFeedbackCommentVo> replies;

    /** 创建时间。 */
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
