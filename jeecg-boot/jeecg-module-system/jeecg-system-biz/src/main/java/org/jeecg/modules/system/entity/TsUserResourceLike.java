package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/** 用户角色与故事点赞实体。 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_user_resource_like")
public class TsUserResourceLike implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 点赞关系主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 点赞用户 ID。 */
    @TableField("user_id")
    private String userId;

    /** 资源类型：role 角色，story 故事。 */
    @TableField("resource_type")
    private String resourceType;

    /** 角色或故事资源 ID。 */
    @TableField("resource_id")
    private Long resourceId;

    /** 状态：1 已点赞，0 已取消。 */
    private Integer status;

    /** 点赞时间。 */
    @TableField("created_at")
    private Date createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private Date updatedAt;
}
