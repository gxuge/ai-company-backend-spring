package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsad.TsAdEventReportDto;
import org.jeecg.modules.system.service.ITsAdDeliveryService;
import org.jeecg.modules.system.vo.tsad.TsAdSlotDeliveryVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

/** 前端海报与广告投放接口。 */
@Tag(name = "TsAds 前端海报与广告")
@RestController
@Validated
@RequestMapping("/sys/ts-ads")
public class TsAdController {
    private final ITsAdDeliveryService adDeliveryService;

    /** 注入广告投放服务。 */
    public TsAdController(ITsAdDeliveryService adDeliveryService) {
        this.adDeliveryService = adDeliveryService;
    }

    /** 登录用户按广告位批量读取当前可见内容。 */
    @Operation(summary = "登录用户读取广告")
    @GetMapping("/delivery")
    public Result<List<TsAdSlotDeliveryVo>> deliver(
            @RequestParam("slotCodes") String slotCodes,
            @RequestParam(value = "platform", defaultValue = "WEB") String platform) {
        return Result.OK(adDeliveryService.deliver(
                splitSlotCodes(slotCodes), platform, currentUser()));
    }

    /** 匿名用户按广告位批量读取当前可见内容。 */
    @Operation(summary = "匿名用户读取广告")
    @GetMapping("/public/delivery")
    public Result<List<TsAdSlotDeliveryVo>> deliverPublic(
            @RequestParam("slotCodes") String slotCodes,
            @RequestParam(value = "platform", defaultValue = "WEB") String platform) {
        return Result.OK(adDeliveryService.deliver(splitSlotCodes(slotCodes), platform, null));
    }

    /** 登录用户幂等上报曝光或点击事件。 */
    @Operation(summary = "登录用户上报广告事件")
    @PostMapping("/event")
    public Result<Boolean> reportEvent(
            @Validated @RequestBody TsAdEventReportDto request) {
        return Result.OK(adDeliveryService.reportEvent(request, currentUser()));
    }

    /** 匿名用户幂等上报曝光或点击事件。 */
    @Operation(summary = "匿名用户上报广告事件")
    @PostMapping("/public/event")
    public Result<Boolean> reportPublicEvent(
            @Validated @RequestBody TsAdEventReportDto request) {
        return Result.OK(adDeliveryService.reportEvent(request, null));
    }

    /** 将逗号分隔广告位编码拆分为列表。 */
    private List<String> splitSlotCodes(String value) {
        return Arrays.asList(value.split(","));
    }

    /** 获取当前登录用户。 */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
