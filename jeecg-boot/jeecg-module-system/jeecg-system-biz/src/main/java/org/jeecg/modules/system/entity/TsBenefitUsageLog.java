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
 * 权益消耗记录。
 */
@Data
@Accessors(chain = true)
@TableName("benefit_usage_log")
public class TsBenefitUsageLog implements Serializable {

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
    /** 本次消耗数量。 */
    @TableField("consume_amount")
    private Integer consumeAmount;
    /** 业务类型。 */
    @TableField("biz_type")
    private String bizType;
    /** 业务唯一 ID。 */
    @TableField("biz_id")
    private String bizId;
    /** 创建时间。 */
    @TableField("created_at")
    private Date createdAt;
}
