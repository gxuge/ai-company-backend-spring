package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserresourcelike.TsUserResourceLikeActionDto;
import org.jeecg.modules.system.dto.tsuserresourcelike.TsUserResourceLikeQueryDto;
import org.jeecg.modules.system.entity.TsUserResourceLike;
import org.jeecg.modules.system.mapper.TsUserResourceLikeMapper;
import org.jeecg.modules.system.po.tsuserresourcelike.TsUserResourceLikeQueryPo;
import org.jeecg.modules.system.service.ITsUserResourceLikeService;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceResolver;
import org.jeecg.modules.system.vo.tsuserresourcelike.TsUserResourceLikeStatusVo;
import org.jeecg.modules.system.vo.tsuserresourcelike.TsUserResourceLikeVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/** 用户角色与故事点赞业务服务实现。 */
@Service
public class TsUserResourceLikeServiceImpl
        extends ServiceImpl<TsUserResourceLikeMapper, TsUserResourceLike>
        implements ITsUserResourceLikeService {

    /** {@inheritDoc} */
    @Override
    public Result<Page<TsUserResourceLikeVo>> pageLikes(
            LoginUser user, TsUserResourceLikeQueryDto request) {
        TsUserResourceLikeQueryPo query =
                TsUserResourceLikeQueryPo.fromRequest(user.getId(), request);
        Page<TsUserResourceLikeVo> page =
                new Page<>(query.getPageNo(), query.getPageSize());
        Page<TsUserResourceLikeVo> pageData = baseMapper.selectLikePage(page, query);
        enrichImageResources(pageData.getRecords());
        return Result.OK(pageData);
    }

    /** {@inheritDoc} */
    @Override
    public Result<TsUserResourceLikeStatusVo> getLikeStatus(
            LoginUser user, TsUserResourceLikeActionDto request) {
        ensureAvailableResource(request);
        return Result.OK(buildStatus(user.getId(), request));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserResourceLikeStatusVo> like(
            LoginUser user, TsUserResourceLikeActionDto request) {
        ensureAvailableResource(request);
        baseMapper.upsertLike(
                user.getId(), request.getResourceType(), request.getResourceId(), new Date());
        return Result.OK("点赞成功", buildStatus(user.getId(), request));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserResourceLikeStatusVo> unlike(
            LoginUser user, TsUserResourceLikeActionDto request) {
        baseMapper.cancelLike(
                user.getId(), request.getResourceType(), request.getResourceId(), new Date());
        return Result.OK("取消点赞成功", buildStatus(user.getId(), request));
    }

    /** 校验资源存在在线公开记录。 */
    private void ensureAvailableResource(TsUserResourceLikeActionDto request) {
        if (baseMapper.countAvailableResource(
                request.getResourceType(), request.getResourceId()) <= 0) {
            throw new JeecgBootException("资源不存在、已下架或不可点赞");
        }
    }

    /** 构造指定资源的点赞状态和实时计数。 */
    private TsUserResourceLikeStatusVo buildStatus(
            String userId, TsUserResourceLikeActionDto request) {
        boolean liked = baseMapper.countActiveLike(
                userId, request.getResourceType(), request.getResourceId()) > 0;
        long likeCount = baseMapper.countResourceLikes(
                request.getResourceType(), request.getResourceId());
        return TsUserResourceLikeStatusVo.of(
                request.getResourceType(), request.getResourceId(), liked, likeCount);
    }

    /** 补充点赞列表的统一图片语义。 */
    private void enrichImageResources(List<TsUserResourceLikeVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (TsUserResourceLikeVo item : records) {
            if ("role".equals(item.getResourceType())) {
                item.setImageResources(TsImageResourceResolver.buildRolePublicBrowseImageResources(
                        item.getResourceId(),
                        item.getAvatarUrl(),
                        item.getCoverUrl(),
                        item.getAuthorAvatar()));
            } else if ("story".equals(item.getResourceType())) {
                item.setImageResources(TsImageResourceResolver.buildStoryPublicBrowseImageResources(
                        item.getResourceId(),
                        item.getSceneImageUrl(),
                        item.getCoverUrl(),
                        item.getAuthorAvatar()));
            }
        }
    }
}
