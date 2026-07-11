package org.jeecg.modules.airag.kb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.airag.kb.entity.KbFederatedRetrievalLog;
import org.jeecg.modules.airag.kb.mapper.KbFederatedRetrievalLogMapper;
import org.jeecg.modules.airag.kb.service.IKbFederatedRetrievalLogService;
import org.springframework.stereotype.Service;

/**
 * 多知识库检索日志服务实现。
 */
@Service
public class KbFederatedRetrievalLogServiceImpl extends ServiceImpl<KbFederatedRetrievalLogMapper, KbFederatedRetrievalLog> implements IKbFederatedRetrievalLogService {
}
