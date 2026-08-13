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
 * 反馈中心点赞实体。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_feedback_like")
public class TsFeedbackLike implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 点赞主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 点赞用户 ID。 */
    @TableField("user_id")
    private String userId;

    /** 点赞目标类型：feedback 或 comment。 */
    @TableField("target_type")
    private String targetType;

    /** 点赞目标 ID。 */
    @TableField("target_id")
    private Long targetId;

    /** 创建时间。 */
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
}
