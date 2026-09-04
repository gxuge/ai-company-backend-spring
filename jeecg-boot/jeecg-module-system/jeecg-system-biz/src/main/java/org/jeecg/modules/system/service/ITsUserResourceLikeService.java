package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserresourcelike.TsUserResourceLikeActionDto;
import org.jeecg.modules.system.dto.tsuserresourcelike.TsUserResourceLikeQueryDto;
import org.jeecg.modules.system.entity.TsUserResourceLike;
import org.jeecg.modules.system.vo.tsuserresourcelike.TsUserResourceLikeStatusVo;
import org.jeecg.modules.system.vo.tsuserresourcelike.TsUserResourceLikeVo;

/** 用户角色与故事点赞业务服务。 */
public interface ITsUserResourceLikeService extends IService<TsUserResourceLike> {

    /** 分页查询当前用户点赞的资源。 */
    Result<Page<TsUserResourceLikeVo>> pageLikes(
            LoginUser user, TsUserResourceLikeQueryDto request);

    /** 查询当前用户对指定资源的点赞状态。 */
    Result<TsUserResourceLikeStatusVo> getLikeStatus(
            LoginUser user, TsUserResourceLikeActionDto request);

    /** 点赞在线公开角色或故事。 */
    Result<TsUserResourceLikeStatusVo> like(
            LoginUser user, TsUserResourceLikeActionDto request);

    /** 取消当前用户对指定资源的点赞。 */
    Result<TsUserResourceLikeStatusVo> unlike(
            LoginUser user, TsUserResourceLikeActionDto request);
}
