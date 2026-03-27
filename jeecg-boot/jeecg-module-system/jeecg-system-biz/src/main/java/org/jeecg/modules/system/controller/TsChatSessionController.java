package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionQueryDto;
import org.jeecg.modules.system.dto.tschatsession.TsChatSessionSaveDto;
import org.jeecg.modules.system.service.ITsChatSessionService;
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
    @Operation(summary = "会话分页查询")
    @GetMapping("/ts-chat-sessions")
    public Result<Page<TsChatSessionVo>> listSessions(TsChatSessionQueryDto request) {
        return tsChatSessionService.pageSessions(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "会话详情")
    @GetMapping("/ts-chat-sessions/detail")
    public Result<TsChatSessionVo> getSession(@RequestParam("id") Long id) {
        return tsChatSessionService.getSession(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "新增会话")
    @PostMapping("/ts-chat-sessions")
    public Result<TsChatSessionVo> createSession(
            @Validated(TsChatSessionSaveDto.Create.class) @RequestBody TsChatSessionSaveDto request) {
        return tsChatSessionService.addSession(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "编辑会话")
    @PutMapping("/ts-chat-sessions")
    public Result<TsChatSessionVo> updateSession(
            @Validated(TsChatSessionSaveDto.Update.class) @RequestBody TsChatSessionSaveDto request) {
        return tsChatSessionService.editSession(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }
    @Operation(summary = "删除会话")
    @DeleteMapping("/ts-chat-sessions")
    public Result<?> removeSession(@RequestParam("id") Long id) {
        return tsChatSessionService.deleteSession(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
}
