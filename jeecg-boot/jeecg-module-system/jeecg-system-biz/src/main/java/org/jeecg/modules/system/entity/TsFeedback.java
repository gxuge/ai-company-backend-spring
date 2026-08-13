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
 * 用户反馈实体。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_feedback")
public class TsFeedback implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 反馈主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 反馈发布用户 ID。 */
    @TableField("user_id")
    private String userId;

    /** 反馈类型：feature、bug、experience。 */
    private String type;

    /** 反馈标题。 */
    private String title;

    /** 反馈内容。 */
    private String content;

    /** 状态：received、processing、completed。 */
    private String status;

    /** 点赞数量。 */
    @TableField("like_count")
    private Integer likeCount;

    /** 评论及回复总数。 */
    @TableField("comment_count")
    private Integer commentCount;

    /** 逻辑删除：0 正常，1 已删除。 */
    @TableLogic(value = "0", delval = "1")
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 创建时间。 */
    @TableField("created_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
