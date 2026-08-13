package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsUserFavorite;
import org.jeecg.modules.system.po.tsuserfavorite.TsUserFavoriteQueryPo;
import org.jeecg.modules.system.vo.tsuserfavorite.TsUserFavoriteVo;

import java.util.Date;

/**
 * 用户收藏数据访问层。
 */
public interface TsUserFavoriteMapper extends BaseMapper<TsUserFavorite> {

    /**
     * 分页查询当前用户有效收藏，并过滤已下架或删除的资源。
     *
     * @param page MyBatis-Plus 分页参数
     * @param query 收藏查询参数
     * @return 收藏展示对象分页
     */
    Page<TsUserFavoriteVo> selectFavoritePage(Page<TsUserFavoriteVo> page,
                                              @Param("query") TsUserFavoriteQueryPo query);

    /**
     * 查询当前用户是否已收藏指定资源。
     *
     * @param userId 当前登录用户 ID
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return 已收藏返回 1，否则返回 0
     */
    int countActiveFavorite(@Param("userId") String userId,
                            @Param("resourceType") String resourceType,
                            @Param("resourceId") Long resourceId);

    /**
     * 校验角色或故事是否存在在线公开记录。
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return 可收藏返回大于 0 的数量
     */
    int countAvailableResource(@Param("resourceType") String resourceType,
                               @Param("resourceId") Long resourceId);

    /**
     * 新增收藏或恢复已取消的收藏。
     *
     * @param userId 当前登录用户 ID
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param now 当前时间
     * @return 受影响行数
     */
    int upsertFavorite(@Param("userId") String userId,
                       @Param("resourceType") String resourceType,
                       @Param("resourceId") Long resourceId,
                       @Param("now") Date now);

    /**
     * 取消当前用户对指定资源的收藏。
     *
     * @param userId 当前登录用户 ID
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param now 当前时间
     * @return 受影响行数
     */
    int cancelFavorite(@Param("userId") String userId,
                       @Param("resourceType") String resourceType,
                       @Param("resourceId") Long resourceId,
                       @Param("now") Date now);
}
