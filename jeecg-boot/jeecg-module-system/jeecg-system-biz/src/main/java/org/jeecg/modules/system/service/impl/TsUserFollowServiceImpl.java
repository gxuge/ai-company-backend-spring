package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserfollow.TsUserFollowActionDto;
import org.jeecg.modules.system.dto.tsuserfollow.TsUserFollowQueryDto;
import org.jeecg.modules.system.entity.TsUserFollow;
import org.jeecg.modules.system.mapper.TsUserFollowMapper;
import org.jeecg.modules.system.po.tsuserfollow.TsUserFollowQueryPo;
import org.jeecg.modules.system.service.ITsUserFollowService;
import org.jeecg.modules.system.vo.tsuserfollow.TsUserFollowStatusVo;
import org.jeecg.modules.system.vo.tsuserfollow.TsUserFollowVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** 用户关注业务服务实现。 */
@Service
public class TsUserFollowServiceImpl
        extends ServiceImpl<TsUserFollowMapper, TsUserFollow>
        implements ITsUserFollowService {

    /** {@inheritDoc} */
    @Override
    public Result<Page<TsUserFollowVo>> pageFollowing(
            LoginUser user, TsUserFollowQueryDto request) {
        TsUserFollowQueryPo query = TsUserFollowQueryPo.fromRequest(user.getId(), request);
        Page<TsUserFollowVo> page = new Page<>(query.getPageNo(), query.getPageSize());
        return Result.OK(baseMapper.selectFollowingPage(page, query));
    }

    /** {@inheritDoc} */
    @Override
    public Result<Page<TsUserFollowVo>> pageFollowers(
            LoginUser user, TsUserFollowQueryDto request) {
        TsUserFollowQueryPo query = TsUserFollowQueryPo.fromRequest(user.getId(), request);
        Page<TsUserFollowVo> page = new Page<>(query.getPageNo(), query.getPageSize());
        return Result.OK(baseMapper.selectFollowerPage(page, query));
    }

    /** {@inheritDoc} */
    @Override
    public Result<TsUserFollowStatusVo> getFollowStatus(
            LoginUser user, TsUserFollowActionDto request) {
        String targetUserId = normalizeTargetUserId(request);
        ensureNotSelf(user.getId(), targetUserId);
        ensureAvailableUser(targetUserId);
        return Result.OK(buildStatus(user.getId(), targetUserId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserFollowStatusVo> follow(
            LoginUser user, TsUserFollowActionDto request) {
        String targetUserId = normalizeTargetUserId(request);
        ensureNotSelf(user.getId(), targetUserId);
        ensureAvailableUser(targetUserId);
        baseMapper.upsertFollow(user.getId(), targetUserId, new Date());
        return Result.OK("关注成功", buildStatus(user.getId(), targetUserId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserFollowStatusVo> unfollow(
            LoginUser user, TsUserFollowActionDto request) {
        String targetUserId = normalizeTargetUserId(request);
        ensureNotSelf(user.getId(), targetUserId);
        baseMapper.cancelFollow(user.getId(), targetUserId, new Date());
        return Result.OK("取消关注成功", buildStatus(user.getId(), targetUserId));
    }

    /** 构造目标用户的关注状态和实时计数。 */
    private TsUserFollowStatusVo buildStatus(String userId, String targetUserId) {
        boolean followed = baseMapper.countActiveFollow(userId, targetUserId) > 0;
        long followerCount = baseMapper.countFollowers(targetUserId);
        long followingCount = baseMapper.countFollowing(targetUserId);
        return TsUserFollowStatusVo.of(
                targetUserId, followed, followerCount, followingCount);
    }

    /** 禁止用户关注自己。 */
    private void ensureNotSelf(String userId, String targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new JeecgBootException("不能关注自己");
        }
    }

    /** 校验目标用户正常且未删除。 */
    private void ensureAvailableUser(String targetUserId) {
        if (baseMapper.countAvailableUser(targetUserId) <= 0) {
            throw new JeecgBootException("目标用户不存在或不可关注");
        }
    }

    /** 归一化目标用户 ID。 */
    private String normalizeTargetUserId(TsUserFollowActionDto request) {
        return request.getTargetUserId().trim();
    }
}
