package org.jeecg.modules.system.service;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tschatsession.TsChatAiReplyDto;
import org.jeecg.modules.system.vo.tschatsession.TsChatAiReplyVo;

public interface ITsChatAiReplyService {

    /**
     * 在指定会话中生成 AI 文本回复并完成语音落库。
     *
     * @param user 当前登录用户
     * @param sessionId 会话 ID
     * @param request 请求参数
     * @return AI 回复结果（文本、语音地址、消息/附件 ID）
     */
    Result<TsChatAiReplyVo> createAiReply(LoginUser user, Long sessionId, TsChatAiReplyDto request);
}
