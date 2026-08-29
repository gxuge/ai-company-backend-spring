package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.exception.JeecgBootException;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.behavior.TsBehaviorEventReporter;
import org.jeecg.modules.system.dto.tsuserfavorite.TsUserFavoriteActionDto;
import org.jeecg.modules.system.dto.tsuserfavorite.TsUserFavoriteQueryDto;
import org.jeecg.modules.system.entity.TsUserFavorite;
import org.jeecg.modules.system.enums.tsbehavior.TsBehaviorEventType;
import org.jeecg.modules.system.mapper.TsUserFavoriteMapper;
import org.jeecg.modules.system.po.tsuserfavorite.TsUserFavoriteQueryPo;
import org.jeecg.modules.system.service.ITsUserFavoriteService;
import org.jeecg.modules.system.vo.tsimage.TsImageResourceResolver;
import org.jeecg.modules.system.vo.tsuserfavorite.TsUserFavoriteStatusVo;
import org.jeecg.modules.system.vo.tsuserfavorite.TsUserFavoriteVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 用户收藏业务服务实现。
 */
@Service
public class TsUserFavoriteServiceImpl extends ServiceImpl<TsUserFavoriteMapper, TsUserFavorite>
        implements ITsUserFavoriteService {

    @Resource
    private TsBehaviorEventReporter behaviorEventReporter;

    /**
     * 分页查询当前用户收藏，仅返回仍在线可访问的角色和故事。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 收藏分页
     */
    @Override
    public Result<Page<TsUserFavoriteVo>> pageFavorites(LoginUser user, TsUserFavoriteQueryDto request) {
        TsUserFavoriteQueryPo queryPo = TsUserFavoriteQueryPo.fromRequest(user.getId(), request);
        Page<TsUserFavoriteVo> page = new Page<>(queryPo.getPageNo(), queryPo.getPageSize());
        Page<TsUserFavoriteVo> pageData = baseMapper.selectFavoritePage(page, queryPo);
        enrichImageResources(pageData.getRecords());
        return Result.OK(pageData);
    }

    /**
     * 查询当前用户对指定资源的收藏状态。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 收藏状态
     */
    @Override
    public Result<TsUserFavoriteStatusVo> getFavoriteStatus(LoginUser user, TsUserFavoriteActionDto request) {
        boolean favorited = baseMapper.countActiveFavorite(
                user.getId(), request.getResourceType(), request.getResourceId()) > 0;
        return Result.OK(TsUserFavoriteStatusVo.of(
                request.getResourceType(), request.getResourceId(), favorited));
    }

    /**
     * 收藏在线公开角色或故事，重复收藏保持成功且不产生重复记录。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 收藏状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserFavoriteStatusVo> addFavorite(LoginUser user, TsUserFavoriteActionDto request) {
        if (baseMapper.countAvailableResource(request.getResourceType(), request.getResourceId()) <= 0) {
            throw new JeecgBootException("资源不存在、已下架或不可收藏");
        }
        baseMapper.upsertFavorite(
                user.getId(), request.getResourceType(), request.getResourceId(), new Date());
        behaviorEventReporter.reportAfterCommit(
                user.getId(),
                TsBehaviorEventType.FAVORITE,
                request.getResourceType(),
                request.getResourceId(),
                Map.of());
        return Result.OK("收藏成功", TsUserFavoriteStatusVo.of(
                request.getResourceType(), request.getResourceId(), true));
    }

    /**
     * 取消当前用户收藏，资源不存在或已取消时仍返回成功。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 收藏状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<TsUserFavoriteStatusVo> cancelFavorite(LoginUser user, TsUserFavoriteActionDto request) {
        baseMapper.cancelFavorite(
                user.getId(), request.getResourceType(), request.getResourceId(), new Date());
        return Result.OK("取消收藏成功", TsUserFavoriteStatusVo.of(
                request.getResourceType(), request.getResourceId(), false));
    }

    /**
     * 根据资源类型补充统一图片语义。
     *
     * @param records 收藏列表
     */
    private void enrichImageResources(List<TsUserFavoriteVo> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        for (TsUserFavoriteVo item : records) {
            if ("role".equals(item.getResourceType())) {
                item.setImageResources(TsImageResourceResolver.buildRolePublicBrowseImageResources(
                        item.getResourceId(), item.getAvatarUrl(), item.getCoverUrl(), item.getAuthorAvatar()));
            } else if ("story".equals(item.getResourceType())) {
                item.setImageResources(TsImageResourceResolver.buildStoryPublicBrowseImageResources(
                        item.getResourceId(), item.getSceneImageUrl(), item.getCoverUrl(), item.getAuthorAvatar()));
            }
        }
    }
}
