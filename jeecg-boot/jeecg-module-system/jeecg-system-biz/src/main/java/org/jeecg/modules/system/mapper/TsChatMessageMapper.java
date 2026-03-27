package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;
import org.jeecg.modules.system.entity.TsChatMessage;
import org.jeecg.modules.system.po.tschatmessage.TsChatMessageQueryPo;
public interface TsChatMessageMapper extends BaseMapper<TsChatMessage> {
    Page<TsChatMessage> selectMessagePage(Page<TsChatMessage> page, @Param("query") TsChatMessageQueryPo query);
    TsChatMessage selectOwnedById(@Param("id") Long id, @Param("userId") String userId);
    Integer selectOwnedSessionCount(@Param("sessionId") Long sessionId, @Param("userId") String userId);
    Long selectNextSeqNoForUpdate(@Param("sessionId") Long sessionId);
}
