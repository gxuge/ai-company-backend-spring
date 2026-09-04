package org.jeecg.modules.system.vo.tsuserfollow;

import lombok.Data;

import java.util.Date;

/** 用户关注列表展示对象。 */
@Data
public class TsUserFollowVo {

    /** 关注关系主键。 */
    private Long followId;

    /** 列表中的用户 ID。 */
    private String userId;

    /** 用户账号。 */
    private String username;

    /** 用户展示名称。 */
    private String displayName;

    /** 用户头像。 */
    private String avatar;

    /** 用户个性签名。 */
    private String sign;

    /** 关注时间。 */
    private Date followedAt;
}
