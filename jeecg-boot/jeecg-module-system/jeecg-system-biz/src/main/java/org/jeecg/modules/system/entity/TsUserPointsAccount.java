package org.jeecg.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/** 用户积分账户。 */
@Data
@Accessors(chain = true)
@TableName("user_points_account")
public class TsUserPointsAccount {
    /** 主键。 */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户ID。 */
    private String userId;
    /** 当前积分余额。 */
    private Long balance;
    /** 累计获得积分。 */
    private Long totalIncome;
    /** 累计消费积分。 */
    private Long totalExpense;
    /** 乐观锁版本号。 */
    private Integer version;
    /** 创建时间。 */
    private Date createdAt;
    /** 更新时间。 */
    private Date updatedAt;
}
