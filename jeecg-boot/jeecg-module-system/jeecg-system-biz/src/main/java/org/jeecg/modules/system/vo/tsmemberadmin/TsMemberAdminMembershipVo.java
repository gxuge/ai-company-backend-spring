package org.jeecg.modules.system.vo.tsmemberadmin;

import lombok.Data;

import java.util.Date;

/** 用户会员后台列表项。 */
@Data
public class TsMemberAdminMembershipVo {
    /** 会员记录 ID。 */
    private Long id;
    /** 用户 ID。 */
    private String userId;
    /** 登录账号。 */
    private String username;
    /** 用户姓名。 */
    private String realname;
    /** 会员等级 ID。 */
    private Long planId;
    /** 会员等级编码。 */
    private String planCode;
    /** 会员等级名称。 */
    private String planName;
    /** 套餐 ID。 */
    private Long productId;
    /** 套餐周期。 */
    private String cycleType;
    /** 生效时间。 */
    private Date startTime;
    /** 到期时间。 */
    private Date endTime;
    /** 状态：0失效，1有效。 */
    private Integer status;
    /** 自动续费：0关闭，1开启。 */
    private Integer autoRenew;
    /** 创建时间。 */
    private Date createdAt;
}
