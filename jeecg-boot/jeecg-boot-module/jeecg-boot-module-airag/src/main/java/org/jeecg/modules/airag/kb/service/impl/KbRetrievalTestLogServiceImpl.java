package org.jeecg.modules.airag.kb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.jeecg.modules.airag.kb.entity.KbRetrievalTestLog;
import org.jeecg.modules.airag.kb.mapper.KbRetrievalTestLogMapper;
import org.jeecg.modules.airag.kb.service.IKbRetrievalTestLogService;
import org.springframework.stereotype.Service;

/**
 * 检索测试日志服务实现。
 */
@Service
public class KbRetrievalTestLogServiceImpl extends ServiceImpl<KbRetrievalTestLogMapper, KbRetrievalTestLog> implements IKbRetrievalTestLogService {
}
