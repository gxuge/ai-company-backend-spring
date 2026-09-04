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

/** 用户关注关系实体。 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ts_user_follow")
public class TsUserFollow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关注关系主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 发起关注的用户 ID。 */
    @TableField("follower_user_id")
    private String followerUserId;

    /** 被关注的用户 ID。 */
    @TableField("followed_user_id")
    private String followedUserId;

    /** 状态：1 已关注，0 已取消。 */
    private Integer status;

    /** 关注时间。 */
    @TableField("created_at")
    private Date createdAt;

    /** 更新时间。 */
    @TableField("updated_at")
    private Date updatedAt;
}
