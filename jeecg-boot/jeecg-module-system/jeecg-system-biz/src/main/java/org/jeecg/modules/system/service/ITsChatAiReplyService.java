package org.jeecg.modules.system.service;

import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tschatsession.TsChatAiReplyDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatReplySuggestionsDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatTemplateReplyDto;
import org.jeecg.modules.system.vo.tschatsession.TsChatAiReplyVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatReplySuggestionsVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatTemplateReplyVo;

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

    /**
     * 在指定会话中基于角色卡、故事卡与历史消息生成模板驱动文本回复。
     *
     * @param user 当前登录用户
     * @param sessionId 会话 ID
     * @param request 请求参数
     * @return 模板驱动聊天回复结果
     */
    Result<TsChatTemplateReplyVo> createTemplateAiReply(LoginUser user, Long sessionId, TsChatTemplateReplyDto request);

    /**
     * 在指定会话中生成 3 条可直接发送的候选回复。
     *
     * @param user 当前登录用户
     * @param sessionId 会话 ID
     * @param request 请求参数
     * @return 候选回复结果
     */
    Result<TsChatReplySuggestionsVo> replySuggestions(LoginUser user, Long sessionId, TsChatReplySuggestionsDto request);
}
