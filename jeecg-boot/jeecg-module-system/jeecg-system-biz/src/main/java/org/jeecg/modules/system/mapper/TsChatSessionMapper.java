package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsChatSession;
import org.jeecg.modules.system.po.tschatsession.TsChatSessionQueryPo;
public interface TsChatSessionMapper extends BaseMapper<TsChatSession> {
    Page<TsChatSession> selectSessionPage(Page<TsChatSession> page, @Param("query") TsChatSessionQueryPo query);
    TsChatSession selectOwnedById(@Param("id") Long id, @Param("userId") String userId);
}
