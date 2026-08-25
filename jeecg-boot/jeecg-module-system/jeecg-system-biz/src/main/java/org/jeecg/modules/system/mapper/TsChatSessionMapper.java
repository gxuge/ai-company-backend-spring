package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsChatSession;
import org.jeecg.modules.system.po.tschatsession.TsChatSessionQueryPo;
import org.jeecg.modules.system.vo.tschatsession.TsChatSessionSummaryVo;

import java.util.List;

public interface TsChatSessionMapper extends BaseMapper<TsChatSession> {
    Page<TsChatSession> selectSessionPage(Page<TsChatSession> page, @Param("query") TsChatSessionQueryPo query);
    TsChatSession selectOwnedById(@Param("id") Long id, @Param("userId") String userId);

    /**
     * 批量查询当前用户会话列表的角色和最后消息摘要。
     *
     * @param sessionIds 会话ID列表
     * @param userId 当前用户ID
     * @return 会话摘要列表
     */
    List<TsChatSessionSummaryVo> selectSessionSummaries(
        @Param("sessionIds") List<Long> sessionIds,
        @Param("userId") String userId);
}
