package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 广告投放规则。 */
@Data
@Accessors(chain = true)
@TableName("ts_ad_delivery_rule")
public class TsAdDeliveryRule {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 广告内容ID，一对一。 */
    private Long contentId;
    /** 平台数组JSON。 */
    private String platformJson;
    /** 受众类型：ALL/LOGIN/ANONYMOUS/USER_LIST。 */
    private String audienceType;
    /** 会员等级数组JSON。 */
    private String memberLevelJson;
    /** 指定用户ID数组JSON。 */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String userIdJson;
    /** 创建人。 */
    private String createdBy;
    /** 更新人。 */
    private String updatedBy;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
