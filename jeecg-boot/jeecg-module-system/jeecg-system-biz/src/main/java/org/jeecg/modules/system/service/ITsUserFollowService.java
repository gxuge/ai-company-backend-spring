package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserfollow.TsUserFollowActionDto;
import org.jeecg.modules.system.dto.tsuserfollow.TsUserFollowQueryDto;
import org.jeecg.modules.system.entity.TsUserFollow;
import org.jeecg.modules.system.vo.tsuserfollow.TsUserFollowStatusVo;
import org.jeecg.modules.system.vo.tsuserfollow.TsUserFollowVo;

/** 用户关注业务服务。 */
public interface ITsUserFollowService extends IService<TsUserFollow> {

    /** 分页查询当前用户关注的用户。 */
    Result<Page<TsUserFollowVo>> pageFollowing(
            LoginUser user, TsUserFollowQueryDto request);

    /** 分页查询当前用户的粉丝。 */
    Result<Page<TsUserFollowVo>> pageFollowers(
            LoginUser user, TsUserFollowQueryDto request);

    /** 查询当前用户对目标用户的关注状态。 */
    Result<TsUserFollowStatusVo> getFollowStatus(
            LoginUser user, TsUserFollowActionDto request);

    /** 关注目标用户。 */
    Result<TsUserFollowStatusVo> follow(
            LoginUser user, TsUserFollowActionDto request);

    /** 取消关注目标用户。 */
    Result<TsUserFollowStatusVo> unfollow(
            LoginUser user, TsUserFollowActionDto request);
}
