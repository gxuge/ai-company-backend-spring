package org.jeecg.modules.airag.kb.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.jeecg.modules.airag.agent.runtime.AgentContext;
import org.jeecg.modules.airag.kb.dto.KbRagChatLogQueryDTO;
import org.jeecg.modules.airag.kb.dto.KbRagQuestionDTO;
import org.jeecg.modules.airag.kb.vo.KbRagAnswerVO;
import org.jeecg.modules.airag.kb.vo.KbRagChatLogVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG 问答服务。
 */
public interface IKbRagQaService {
    /**
     * 同步问答。
     *
     * @param dto 请求
     * @return 结果
     */
    KbRagAnswerVO ask(KbRagQuestionDTO dto);

    /**
     * 带Agent上下文的同步问答。
     *
     * @param context Agent上下文
     * @param dto 请求
     * @return 结果
     */
    KbRagAnswerVO ask(AgentContext context, KbRagQuestionDTO dto);

    /**
     * 流式问答。
     *
     * @param dto 请求
     * @return SSE
     */
    SseEmitter askStream(KbRagQuestionDTO dto);

    /**
     * 带Agent上下文的流式问答。
     *
     * @param context Agent上下文
     * @param dto 请求
     * @return SSE
     */
    SseEmitter askStream(AgentContext context, KbRagQuestionDTO dto);

    /**
     * 查询日志列表。
     *
     * @param dto 条件
     * @return 日志分页
     */
    IPage<KbRagChatLogVo> pageLogs(KbRagChatLogQueryDTO dto);

    /**
     * 查询日志详情。
     *
     * @param id 日志ID
     * @return 日志详情
     */
    KbRagChatLogVo getLogById(String id);
}
