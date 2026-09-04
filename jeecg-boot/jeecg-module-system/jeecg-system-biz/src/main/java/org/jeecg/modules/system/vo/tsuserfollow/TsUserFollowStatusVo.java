package org.jeecg.modules.system.vo.tsuserfollow;

import lombok.Data;

/** 用户关注状态对象。 */
@Data
public class TsUserFollowStatusVo {

    /** 目标用户 ID。 */
    private String targetUserId;

    /** 当前登录用户是否已关注目标用户。 */
    private Boolean followed;

    /** 目标用户的粉丝数量。 */
    private Long followerCount;

    /** 目标用户的关注数量。 */
    private Long followingCount;

    /** 创建关注状态对象。 */
    public static TsUserFollowStatusVo of(
            String targetUserId, boolean followed, long followerCount, long followingCount) {
        TsUserFollowStatusVo vo = new TsUserFollowStatusVo();
        vo.setTargetUserId(targetUserId);
        vo.setFollowed(followed);
        vo.setFollowerCount(followerCount);
        vo.setFollowingCount(followingCount);
        return vo;
    }
}
