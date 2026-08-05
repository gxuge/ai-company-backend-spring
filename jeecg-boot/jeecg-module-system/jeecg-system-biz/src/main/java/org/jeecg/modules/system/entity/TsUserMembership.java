package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户会员记录。
 */
@Data
@Accessors(chain = true)
@TableName("user_membership")
public class TsUserMembership implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** JEECG 用户 ID。 */
    @TableField("user_id")
    private String userId;
    /** 会员等级 ID。 */
    @TableField("plan_id")
    private Long planId;
    /** 最近购买的套餐 ID。 */
    @TableField("product_id")
    private Long productId;
    /** 生效时间。 */
    @TableField("start_time")
    private Date startTime;
    /** 到期时间。 */
    @TableField("end_time")
    private Date endTime;
    /** 状态：0失效，1有效。 */
    @TableField("status")
    private Integer status;
    /** 自动续费：0关闭，1开启。 */
    @TableField("auto_renew")
    private Integer autoRenew;
    /** 创建时间。 */
    @TableField("created_at")
    private Date createdAt;
}
