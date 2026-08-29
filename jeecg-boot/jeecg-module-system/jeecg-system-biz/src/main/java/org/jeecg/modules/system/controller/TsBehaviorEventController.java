package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsbehavior.TsBehaviorBatchDto;
import org.jeecg.modules.system.dto.tsbehavior.TsBehaviorEventDto;
import org.jeecg.modules.system.service.ITsBehaviorEventService;
import org.jeecg.modules.system.vo.tsbehavior.TsBehaviorCollectVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 业务行为分析事件采集接口。 */
@Tag(name = "TsBehaviorEvents 业务行为分析")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-events")
public class TsBehaviorEventController {
    private final ITsBehaviorEventService behaviorEventService;

    /** 注入业务行为采集服务。 */
    public TsBehaviorEventController(ITsBehaviorEventService behaviorEventService) {
        this.behaviorEventService = behaviorEventService;
    }

    /** 接收单条登录用户行为并异步投递 Kafka。 */
    @Operation(summary = "单条上报业务行为")
    @PostMapping("/collect")
    public Result<TsBehaviorCollectVo> collect(
            @Valid @RequestBody TsBehaviorEventDto request) {
        return Result.OK(behaviorEventService.collect(currentUser(), List.of(request)));
    }

    /** 接收批量登录用户行为并异步投递 Kafka。 */
    @Operation(summary = "批量上报业务行为")
    @PostMapping("/collect/batch")
    public Result<TsBehaviorCollectVo> collectBatch(
            @Valid @RequestBody TsBehaviorBatchDto request) {
        return Result.OK(
                behaviorEventService.collect(currentUser(), request.getEvents()));
    }

    /** 获取当前登录用户。 */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
