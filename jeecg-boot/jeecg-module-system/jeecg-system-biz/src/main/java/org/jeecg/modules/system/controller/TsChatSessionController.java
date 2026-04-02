package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tschatsession.TsChatAiReplyDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionQueryDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionSaveDto;
import org.jeecg.modules.system.service.ITsChatAiReplyService;
import org.jeecg.modules.system.service.ITsChatSessionService;
import org.jeecg.modules.system.vo.tschatsession.TsChatAiReplyVo;
import org.jeecg.modules.system.vo.tschatsession.TsChatSessionVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Slf4j
@Tag(name = "TsChatSession 会话")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsChatSessionController {

    @Autowired
    private ITsChatSessionService tsChatSessionService;
    @Autowired
    private ITsChatAiReplyService tsChatAiReplyService;

    /**
     * 分页查询当前用户会话列表。
     *
     * @param request 查询参数
     * @return 会话分页数据
     */
    @Operation(summary = "会话分页查询")
    @GetMapping("/ts-chat-sessions")
    public Result<Page<TsChatSessionVo>> listSessions(TsChatSessionQueryDto request) {
        return tsChatSessionService.pageSessions(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }

    /**
     * 查询当前用户会话详情。
     *
     * @param id 会话 ID
     * @return 会话详情
     */
    @Operation(summary = "会话详情")
    @GetMapping("/ts-chat-sessions/detail")
    public Result<TsChatSessionVo> getSession(@RequestParam("id") Long id) {
        return tsChatSessionService.getSession(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }

    /**
     * 新增会话。
     *
     * @param request 新增参数
     * @return 新建会话
     */
    @Operation(summary = "新增会话")
    @PostMapping("/ts-chat-sessions")
    public Result<TsChatSessionVo> createSession(
            @Validated(TsChatSessionSaveDto.Create.class) @RequestBody TsChatSessionSaveDto request) {
        return tsChatSessionService.addSession(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }

    /**
     * 编辑会话。
     *
     * @param request 编辑参数
     * @return 更新后的会话
     */
    @Operation(summary = "编辑会话")
    @PutMapping("/ts-chat-sessions")
    public Result<TsChatSessionVo> updateSession(
            @Validated(TsChatSessionSaveDto.Update.class) @RequestBody TsChatSessionSaveDto request) {
        return tsChatSessionService.editSession(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }

    /**
     * 删除会话。
     *
     * @param id 会话 ID
     * @return 删除结果
     */
    @Operation(summary = "删除会话")
    @DeleteMapping("/ts-chat-sessions")
    public Result<?> removeSession(@RequestParam("id") Long id) {
        return tsChatSessionService.deleteSession(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }

    /**
     * 在指定会话内生成 AI 文本与语音回复。
     *
     * @param request 回复请求参数
     * @return AI 回复结果
     */
    @Operation(summary = "会话内调用 AI 生成文本与语音")
    @PostMapping("/ts-chat-sessions/ai-reply")
    public Result<TsChatAiReplyVo> createAiReply(@Validated @RequestBody TsChatAiReplyDto request) {
        return tsChatAiReplyService.createAiReply(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getSessionId(), request);
    }
}
