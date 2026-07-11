package org.jeecg.modules.airag.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.airag.kb.entity.KbRagChatLog;

/**
 * RAG 问答日志Mapper。
 */
@Mapper
public interface KbRagChatLogMapper extends BaseMapper<KbRagChatLog> {
}
