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
 * 追加反馈实体。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_feedback_append")
public class TsFeedbackAppend implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 追加反馈主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属反馈 ID。 */
    @TableField("feedback_id")
    private Long feedbackId;

    /** 追加用户 ID。 */
    @TableField("user_id")
    private String userId;

    /** 追加内容。 */
    private String content;

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
