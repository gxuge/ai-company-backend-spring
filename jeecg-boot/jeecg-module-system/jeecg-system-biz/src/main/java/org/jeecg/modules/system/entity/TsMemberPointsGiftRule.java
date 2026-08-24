package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 会员开通积分赠送规则。 */
@Data
@Accessors(chain = true)
@TableName("member_points_gift_rule")
public class TsMemberPointsGiftRule {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 会员等级ID。 */
    private Long planId;
    /** 会员套餐ID，0表示等级默认规则。 */
    private Long productId;
    /** 赠送积分。 */
    private Long giftPoints;
    /** 状态：0停用，1启用。 */
    private Integer status;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
