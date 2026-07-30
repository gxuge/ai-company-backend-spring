package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsDraft;
import org.jeecg.modules.system.po.tsdraft.TsDraftQueryPo;

/**
 * 统一草稿数据访问层。
 */
public interface TsDraftMapper extends BaseMapper<TsDraft> {

    /**
     * 分页查询当前用户草稿及页面状态。
     *
     * @param page MyBatis-Plus 分页参数
     * @param query 草稿查询参数
     * @return 草稿实体分页
     */
    Page<TsDraft> selectDraftPage(Page<TsDraft> page, @Param("query") TsDraftQueryPo query);

    /**
     * 查询当前用户拥有的有效草稿。
     *
     * @param id 草稿 ID
     * @param userId 当前登录用户 ID
     * @return 草稿实体，不存在或无权限时返回 null
     */
    TsDraft selectOwned(@Param("id") Long id, @Param("userId") String userId);
}
