package org.jeecg.modules.system.vo.tspoints;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** 后台积分账户响应。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TsPointsAdminAccountVo extends TsPointsAccountVo {
    /** 用户昵称或姓名。 */
    private String nickname;
    /** 用户账号。 */
    private String username;
    /** 用户邮箱。 */
    private String email;
    /** 用户头像。 */
    private String avatar;
    /** 最近变动时间。 */
    private Date updatedAt;
}
