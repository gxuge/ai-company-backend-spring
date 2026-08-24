package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("ts_work_review_log")
public class TsWorkReviewLog implements Serializable {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("review_id")
    private Long reviewId;
    @TableField("action_type")
    private String actionType;
    @TableField("before_status")
    private String beforeStatus;
    @TableField("after_status")
    private String afterStatus;
    @TableField("operator_type")
    private String operatorType;
    @TableField("operator_id")
    private String operatorId;
    private String reason;
    @TableField("created_at")
    private Date createdAt;
}
