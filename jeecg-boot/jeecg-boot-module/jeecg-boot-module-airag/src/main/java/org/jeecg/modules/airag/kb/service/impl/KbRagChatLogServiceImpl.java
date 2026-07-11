package org.jeecg.modules.airag.kb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.airag.kb.entity.KbRagChatLog;
import org.jeecg.modules.airag.kb.mapper.KbRagChatLogMapper;
import org.jeecg.modules.airag.kb.service.IKbRagChatLogService;
import org.springframework.stereotype.Service;

/**
 * RAG 问答日志服务实现。
 */
@Service
public class KbRagChatLogServiceImpl extends ServiceImpl<KbRagChatLogMapper, KbRagChatLog> implements IKbRagChatLogService {
}
