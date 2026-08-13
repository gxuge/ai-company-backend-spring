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
 * 用户角色与故事浏览记录实体。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_user_browse_history")
public class TsUserBrowseHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 浏览记录主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 浏览记录所属用户 ID。 */
    @TableField("user_id")
    private String userId;

    /** 资源类型：role 角色，story 故事。 */
    @TableField("resource_type")
    private String resourceType;

    /** 角色或故事资源 ID。 */
    @TableField("resource_id")
    private Long resourceId;

    /** 累计浏览次数。 */
    @TableField("view_count")
    private Long viewCount;

    /** 首次浏览时间。 */
    @TableField("first_viewed_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date firstViewedAt;

    /** 最近浏览时间。 */
    @TableField("last_viewed_at")
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastViewedAt;

    /** 状态：1 有效，0 已删除。 */
    private Integer status;
}
