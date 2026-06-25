package org.jeecg.modules.system.service;

import org.jeecg.modules.system.dto.tschatsession.TsChatTtsSynthesizeDto;
import org.jeecg.modules.system.vo.tschatsession.TsChatTtsResultVo;

public interface ITsChatTtsGatewayService {

    /**
     * 按当前 AIRAG 应用的语音模型配置即时生成聊天语音，
     * 同时返回稳定 cacheKey 供前端做本地缓存复用。
     *
     * @param request 语音合成参数
     * @return 语音结果
     */
    TsChatTtsResultVo synthesizeForChat(TsChatTtsSynthesizeDto request);
}
