package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsad.TsAdContentQueryDto;
import org.jeecg.modules.system.dto.tsad.TsAdContentSaveDto;
import org.jeecg.modules.system.dto.tsad.TsAdDeliveryRuleSaveDto;
import org.jeecg.modules.system.dto.tsad.TsAdIdDto;
import org.jeecg.modules.system.dto.tsad.TsAdSlotQueryDto;
import org.jeecg.modules.system.dto.tsad.TsAdSlotSaveDto;
import org.jeecg.modules.system.dto.tsad.TsAdStatsQueryDto;
import org.jeecg.modules.system.dto.tsad.TsAdStatusDto;
import org.jeecg.modules.system.service.ITsAdAdminService;
import org.jeecg.modules.system.vo.tsad.TsAdContentVo;
import org.jeecg.modules.system.vo.tsad.TsAdDeliveryRuleVo;
import org.jeecg.modules.system.vo.tsad.TsAdSlotVo;
import org.jeecg.modules.system.vo.tsad.TsAdStatsVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 海报与广告运营后台接口。 */
@Tag(name = "TsAdAdmin 海报与广告运营管理")
@RestController
@Validated
@RequiresRoles("admin")
@RequestMapping("/sys/ts-ad-admin")
public class TsAdAdminController {
    private final ITsAdAdminService adAdminService;

    /** 注入广告运营后台服务。 */
    public TsAdAdminController(ITsAdAdminService adAdminService) {
        this.adAdminService = adAdminService;
    }

    /** 分页查询广告位。 */
    @Operation(summary = "分页查询广告位")
    @PostMapping("/slot/page")
    public Result<Page<TsAdSlotVo>> pageSlots(@RequestBody TsAdSlotQueryDto request) {
        return Result.OK(adAdminService.pageSlots(request));
    }

    /** 查询广告位详情。 */
    @Operation(summary = "查询广告位详情")
    @GetMapping("/slot/detail")
    public Result<TsAdSlotVo> getSlot(@RequestParam("id") Long id) {
        return Result.OK(adAdminService.getSlot(id));
    }

    /** 创建广告位。 */
    @Operation(summary = "创建广告位")
    @PostMapping("/slot/create")
    public Result<Long> createSlot(@Validated @RequestBody TsAdSlotSaveDto request) {
        return Result.OK(adAdminService.createSlot(request, operator()));
    }

    /** 更新广告位。 */
    @Operation(summary = "更新广告位")
    @PostMapping("/slot/update")
    public Result<Void> updateSlot(@Validated @RequestBody TsAdSlotSaveDto request) {
        adAdminService.updateSlot(request, operator());
        return Result.OK("保存成功");
    }

    /** 删除没有内容的广告位。 */
    @Operation(summary = "删除广告位")
    @PostMapping("/slot/delete")
    public Result<Void> deleteSlot(@Validated @RequestBody TsAdIdDto request) {
        adAdminService.deleteSlot(request.getId(), operator());
        return Result.OK("删除成功");
    }

    /** 启用或停用广告位。 */
    @Operation(summary = "更新广告位状态")
    @PostMapping("/slot/status")
    public Result<Void> updateSlotStatus(@Validated @RequestBody TsAdStatusDto request) {
        adAdminService.updateSlotStatus(request.getId(), request.getStatus(), operator());
        return Result.OK("保存成功");
    }

    /** 分页查询广告内容。 */
    @Operation(summary = "分页查询广告内容")
    @PostMapping("/content/page")
    public Result<Page<TsAdContentVo>> pageContents(
            @RequestBody TsAdContentQueryDto request) {
        return Result.OK(adAdminService.pageContents(request));
    }

    /** 查询广告内容详情。 */
    @Operation(summary = "查询广告内容详情")
    @GetMapping("/content/detail")
    public Result<TsAdContentVo> getContent(@RequestParam("id") Long id) {
        return Result.OK(adAdminService.getContent(id));
    }

    /** 创建广告内容。 */
    @Operation(summary = "创建广告内容")
    @PostMapping("/content/create")
    public Result<Long> createContent(@Validated @RequestBody TsAdContentSaveDto request) {
        return Result.OK(adAdminService.createContent(request, operator()));
    }

    /** 更新广告内容并回到草稿。 */
    @Operation(summary = "更新广告内容")
    @PostMapping("/content/update")
    public Result<Void> updateContent(@Validated @RequestBody TsAdContentSaveDto request) {
        adAdminService.updateContent(request, operator());
        return Result.OK("保存成功");
    }

    /** 删除广告内容。 */
    @Operation(summary = "删除广告内容")
    @PostMapping("/content/delete")
    public Result<Void> deleteContent(@Validated @RequestBody TsAdIdDto request) {
        adAdminService.deleteContent(request.getId(), operator());
        return Result.OK("删除成功");
    }

    /** 发布广告内容。 */
    @Operation(summary = "发布广告内容")
    @PostMapping("/content/publish")
    public Result<Void> publishContent(@Validated @RequestBody TsAdIdDto request) {
        adAdminService.publishContent(request.getId(), operator());
        return Result.OK("发布成功");
    }

    /** 下线广告内容。 */
    @Operation(summary = "下线广告内容")
    @PostMapping("/content/offline")
    public Result<Void> offlineContent(@Validated @RequestBody TsAdIdDto request) {
        adAdminService.offlineContent(request.getId(), operator());
        return Result.OK("下线成功");
    }

    /** 查询广告投放规则。 */
    @Operation(summary = "查询广告投放规则")
    @GetMapping("/delivery-rule")
    public Result<TsAdDeliveryRuleVo> getDeliveryRule(
            @RequestParam("contentId") Long contentId) {
        return Result.OK(adAdminService.getDeliveryRule(contentId));
    }

    /** 保存广告投放规则。 */
    @Operation(summary = "保存广告投放规则")
    @PostMapping("/delivery-rule/save")
    public Result<Void> saveDeliveryRule(
            @Validated @RequestBody TsAdDeliveryRuleSaveDto request) {
        adAdminService.saveDeliveryRule(request, operator());
        return Result.OK("保存成功");
    }

    /** 查询广告曝光点击汇总。 */
    @Operation(summary = "查询广告曝光点击汇总")
    @PostMapping("/stats/summary")
    public Result<TsAdStatsVo> getStats(@RequestBody TsAdStatsQueryDto request) {
        return Result.OK(adAdminService.getStats(request));
    }

    /** 获取当前管理员账号。 */
    private String operator() {
        LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
        return user == null ? null : user.getUsername();
    }
}
