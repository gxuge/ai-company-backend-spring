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
 * 用户权益额度。
 */
@Data
@Accessors(chain = true)
@TableName("user_benefit_quota")
public class TsUserBenefitQuota implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键。 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /** JEECG 用户 ID。 */
    @TableField("user_id")
    private String userId;
    /** 权益编码。 */
    @TableField("benefit_code")
    private String benefitCode;
    /** 总额度，-1 表示无限。 */
    @TableField("total_amount")
    private Integer totalAmount;
    /** 已使用额度。 */
    @TableField("used_amount")
    private Integer usedAmount;
    /** 额度到期时间。 */
    @TableField("expire_time")
    private Date expireTime;
    /** 创建时间。 */
    @TableField("created_at")
    private Date createdAt;
}
