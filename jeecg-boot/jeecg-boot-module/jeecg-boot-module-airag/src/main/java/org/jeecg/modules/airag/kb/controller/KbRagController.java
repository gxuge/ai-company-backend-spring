package org.jeecg.modules.airag.kb.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.airag.kb.dto.KbRagChatLogQueryDTO;
import org.jeecg.modules.airag.kb.dto.KbRagQuestionDTO;
import org.jeecg.modules.airag.kb.service.IKbRagQaService;
import org.jeecg.modules.airag.kb.vo.KbRagAnswerVO;
import org.jeecg.modules.airag.kb.vo.KbRagChatLogVo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG 问答控制器。
 */
@Tag(name = "RAG 问答")
@RestController
@RequestMapping("/kb/rag")
public class KbRagController {
    /**
     * RAG 问答服务。
     */
    private final IKbRagQaService kbRagQaService;

    /**
     * 构造函数。
     *
     * @param kbRagQaService RAG 问答服务
     */
    public KbRagController(IKbRagQaService kbRagQaService) {
        this.kbRagQaService = kbRagQaService;
    }

    /**
     * 执行问答。
     *
     * @param dto 请求
     * @return 结果或流式输出
     */
    @PostMapping("/ask")
    @Operation(summary = "执行RAG问答")
    public Object ask(@Valid @RequestBody KbRagQuestionDTO dto) {
        if (Boolean.TRUE.equals(dto.getStream())) {
            return kbRagQaService.askStream(dto);
        }
        KbRagAnswerVO answer = kbRagQaService.ask(dto);
        return Result.OK(answer);
    }

    /**
     * 流式问答。
     *
     * @param dto 请求
     * @return SSE
     */
    @PostMapping("/stream")
    @Operation(summary = "流式RAG问答")
    public SseEmitter askStream(@Valid @RequestBody KbRagQuestionDTO dto) {
        return kbRagQaService.askStream(dto);
    }

    /**
     * 日志列表。
     *
     * @param dto 查询条件
     * @return 日志列表
     */
    @GetMapping("/logs")
    @Operation(summary = "查询RAG问答日志列表")
    public Result<IPage<KbRagChatLogVo>> pageLogs(@Valid KbRagChatLogQueryDTO dto) {
        return Result.OK(kbRagQaService.pageLogs(dto));
    }

    /**
     * 日志详情。
     *
     * @param id 日志ID
     * @return 日志详情
     */
    @GetMapping("/logs/{id}")
    @Operation(summary = "查询RAG问答日志详情")
    public Result<KbRagChatLogVo> getLogById(@PathVariable("id") String id) {
        return Result.OK(kbRagQaService.getLogById(id));
    }
}
