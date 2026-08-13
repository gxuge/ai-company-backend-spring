package org.jeecg.modules.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsuserfavorite.TsUserFavoriteActionDto;
import org.jeecg.modules.system.dto.tsuserfavorite.TsUserFavoriteQueryDto;
import org.jeecg.modules.system.entity.TsUserFavorite;
import org.jeecg.modules.system.vo.tsuserfavorite.TsUserFavoriteStatusVo;
import org.jeecg.modules.system.vo.tsuserfavorite.TsUserFavoriteVo;

/**
 * 用户收藏业务服务。
 */
public interface ITsUserFavoriteService extends IService<TsUserFavorite> {

    /**
     * 分页查询当前用户收藏。
     *
     * @param user 当前登录用户
     * @param request 查询参数
     * @return 收藏分页
     */
    Result<Page<TsUserFavoriteVo>> pageFavorites(LoginUser user, TsUserFavoriteQueryDto request);

    /**
     * 查询当前用户对指定资源的收藏状态。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 收藏状态
     */
    Result<TsUserFavoriteStatusVo> getFavoriteStatus(LoginUser user, TsUserFavoriteActionDto request);

    /**
     * 收藏在线公开角色或故事。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 收藏状态
     */
    Result<TsUserFavoriteStatusVo> addFavorite(LoginUser user, TsUserFavoriteActionDto request);

    /**
     * 取消当前用户收藏。
     *
     * @param user 当前登录用户
     * @param request 资源参数
     * @return 收藏状态
     */
    Result<TsUserFavoriteStatusVo> cancelFavorite(LoginUser user, TsUserFavoriteActionDto request);
}
