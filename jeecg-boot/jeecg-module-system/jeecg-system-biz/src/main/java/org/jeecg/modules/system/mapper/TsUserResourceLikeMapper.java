package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsUserResourceLike;
import org.jeecg.modules.system.po.tsuserresourcelike.TsUserResourceLikeQueryPo;
import org.jeecg.modules.system.vo.tsuserresourcelike.TsUserResourceLikeVo;

import java.util.Date;

/** 用户角色与故事点赞数据访问层。 */
public interface TsUserResourceLikeMapper extends BaseMapper<TsUserResourceLike> {

    /** 分页查询当前用户仍可访问的点赞资源。 */
    Page<TsUserResourceLikeVo> selectLikePage(
            Page<TsUserResourceLikeVo> page,
            @Param("query") TsUserResourceLikeQueryPo query);

    /** 查询当前用户是否已点赞指定资源。 */
    int countActiveLike(
            @Param("userId") String userId,
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId);

    /** 校验角色或故事是否存在在线公开记录。 */
    int countAvailableResource(
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId);

    /** 查询指定资源有效点赞总数。 */
    long countResourceLikes(
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId);

    /** 新增点赞或恢复已取消的点赞。 */
    int upsertLike(
            @Param("userId") String userId,
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId,
            @Param("now") Date now);

    /** 取消当前用户对指定资源的有效点赞。 */
    int cancelLike(
            @Param("userId") String userId,
            @Param("resourceType") String resourceType,
            @Param("resourceId") Long resourceId,
            @Param("now") Date now);
}
