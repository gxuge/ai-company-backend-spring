package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsUserBrowseHistory;
import org.jeecg.modules.system.po.tsuserbrowsehistory.TsUserBrowseHistoryQueryPo;
import org.jeecg.modules.system.vo.tsuserbrowsehistory.TsUserBrowseHistoryVo;

import java.util.Date;

/**
 * 用户浏览记录数据访问层。
 */
public interface TsUserBrowseHistoryMapper extends BaseMapper<TsUserBrowseHistory> {

    /**
     * 分页查询当前用户有效浏览记录，并过滤已下架或删除的资源。
     *
     * @param page MyBatis-Plus 分页参数
     * @param query 浏览记录查询参数
     * @return 浏览记录分页
     */
    Page<TsUserBrowseHistoryVo> selectHistoryPage(Page<TsUserBrowseHistoryVo> page,
                                                  @Param("query") TsUserBrowseHistoryQueryPo query);

    /**
     * 查询当前用户指定资源的有效浏览记录。
     *
     * @param userId 当前登录用户 ID
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return 浏览记录，不存在时返回 null
     */
    TsUserBrowseHistory selectActiveHistory(@Param("userId") String userId,
                                            @Param("resourceType") String resourceType,
                                            @Param("resourceId") Long resourceId);

    /**
     * 校验角色或故事是否存在在线公开记录。
     *
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return 可记录时返回大于 0 的数量
     */
    int countAvailableResource(@Param("resourceType") String resourceType,
                               @Param("resourceId") Long resourceId);

    /**
     * 新增浏览记录或累加已有记录。
     *
     * @param userId 当前登录用户 ID
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @param now 当前时间
     * @return 受影响行数
     */
    int upsertHistory(@Param("userId") String userId,
                      @Param("resourceType") String resourceType,
                      @Param("resourceId") Long resourceId,
                      @Param("now") Date now);

    /**
     * 软删除当前用户指定浏览记录。
     *
     * @param userId 当前登录用户 ID
     * @param resourceType 资源类型
     * @param resourceId 资源 ID
     * @return 受影响行数
     */
    int deleteHistory(@Param("userId") String userId,
                      @Param("resourceType") String resourceType,
                      @Param("resourceId") Long resourceId);

    /**
     * 清空当前用户全部有效浏览记录。
     *
     * @param userId 当前登录用户 ID
     * @return 受影响行数
     */
    int clearHistory(@Param("userId") String userId);
}
