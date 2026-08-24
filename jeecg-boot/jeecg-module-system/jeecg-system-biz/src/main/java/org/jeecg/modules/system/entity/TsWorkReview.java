package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("ts_work_review")
public class TsWorkReview implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("review_no")
    private String reviewNo;
    @TableField("work_type")
    private String workType;
    @TableField("work_id")
    private Long workId;
    @TableField("owner_user_id")
    private String ownerUserId;
    @TableField("work_version")
    private Integer workVersion;
    @TableField("requested_public")
    private Integer requestedPublic;
    @TableField("snapshot_json")
    private String snapshotJson;
    @TableField("snapshot_hash")
    private String snapshotHash;
    private String status;
    @TableField("ai_decision")
    private String aiDecision;
    @TableField("ai_risk_level")
    private String aiRiskLevel;
    @TableField("ai_reason")
    private String aiReason;
    @TableField("ai_result_json")
    private String aiResultJson;
    @TableField("ai_reviewed_at")
    private Date aiReviewedAt;
    @TableField("admin_reviewer_id")
    private String adminReviewerId;
    @TableField("admin_reason")
    private String adminReason;
    @TableField("admin_reviewed_at")
    private Date adminReviewedAt;
    @TableField("submitted_at")
    private Date submittedAt;
    @TableField("created_at")
    private Date createdAt;
    @TableField("updated_at")
    private Date updatedAt;
}
