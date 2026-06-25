package org.jeecg.modules.system.service;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsagentchatsession.TsAgentChatReplyDto;
import org.jeecg.modules.system.vo.tsagentchatsession.TsAgentChatReplyVo;

/**
 * Agent 回复编排服务。
 *
 * @author codex
 * @date 2026/6/25
 */
public interface ITsAgentChatReplyService {

    /**
     * 生成一次 Agent 回复。
     *
     * @param user 当前用户
     * @param sessionId 会话ID
     * @param request 请求参数
     * @return 回复结果
     */
    Result<TsAgentChatReplyVo> createAiReply(LoginUser user, Long sessionId, TsAgentChatReplyDto request);
}
