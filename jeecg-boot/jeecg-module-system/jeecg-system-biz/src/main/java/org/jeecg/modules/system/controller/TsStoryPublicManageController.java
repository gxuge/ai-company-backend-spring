package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicActionDto;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicQueryDto;
import org.jeecg.modules.system.dto.tsstorypublic.TsStoryPublicSaveDto;
import org.jeecg.modules.system.service.ITsStoryPublicService;
import org.jeecg.modules.system.vo.tsstorypublic.TsStoryPublicTargetOptionVo;
import org.jeecg.modules.system.vo.tsstorypublic.TsStoryPublicVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 故事公开记录管理接口。
 */
@Slf4j
@Tag(name = "TsStoryPublic 故事公开记录")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsStoryPublicManageController {

    private final ITsStoryPublicService tsStoryPublicService;

    public TsStoryPublicManageController(ITsStoryPublicService tsStoryPublicService) {
        this.tsStoryPublicService = tsStoryPublicService;
    }

    @Operation(summary = "故事公开记录分页查询")
    @GetMapping("/ts-story-publics")
    public Result<Page<TsStoryPublicVo>> listPublics(TsStoryPublicQueryDto request) {
        return tsStoryPublicService.pagePublics(currentUser(), request);
    }

    @Operation(summary = "故事公开记录详情")
    @GetMapping("/ts-story-publics/detail")
    public Result<TsStoryPublicVo> getPublic(@RequestParam("id") Long id) {
        return tsStoryPublicService.getPublic(currentUser(), id);
    }

    @Operation(summary = "新增故事公开记录")
    @PostMapping("/ts-story-publics")
    public Result<TsStoryPublicVo> createPublic(
            @Validated(TsStoryPublicSaveDto.Create.class) @RequestBody TsStoryPublicSaveDto request) {
        return tsStoryPublicService.addPublic(currentUser(), request);
    }

    @Operation(summary = "编辑故事公开记录")
    @PutMapping("/ts-story-publics")
    public Result<TsStoryPublicVo> updatePublic(
            @Validated(TsStoryPublicSaveDto.Update.class) @RequestBody TsStoryPublicSaveDto request) {
        return tsStoryPublicService.editPublic(currentUser(), request.getId(), request);
    }

    @Operation(summary = "删除故事公开记录")
    @DeleteMapping("/ts-story-publics")
    public Result<?> deletePublic(@RequestParam("id") Long id) {
        return tsStoryPublicService.deletePublic(currentUser(), id);
    }

    @Operation(summary = "提交故事公开记录")
    @PostMapping("/ts-story-publics/submit")
    public Result<TsStoryPublicVo> submit(@Validated @RequestBody TsStoryPublicActionDto request) {
        return tsStoryPublicService.submitPublic(currentUser(), request);
    }

    @Operation(summary = "审核通过故事公开记录")
    @PostMapping("/ts-story-publics/approve")
    public Result<TsStoryPublicVo> approve(@Validated @RequestBody TsStoryPublicActionDto request) {
        return tsStoryPublicService.approvePublic(currentUser(), request);
    }

    @Operation(summary = "驳回故事公开记录")
    @PostMapping("/ts-story-publics/reject")
    public Result<TsStoryPublicVo> reject(@Validated @RequestBody TsStoryPublicActionDto request) {
        return tsStoryPublicService.rejectPublic(currentUser(), request);
    }

    @Operation(summary = "上架故事公开记录")
    @PostMapping("/ts-story-publics/online")
    public Result<TsStoryPublicVo> online(@Validated @RequestBody TsStoryPublicActionDto request) {
        return tsStoryPublicService.onlinePublic(currentUser(), request);
    }

    @Operation(summary = "下架故事公开记录")
    @PostMapping("/ts-story-publics/offline")
    public Result<TsStoryPublicVo> offline(@Validated @RequestBody TsStoryPublicActionDto request) {
        return tsStoryPublicService.offlinePublic(currentUser(), request);
    }

    @Operation(summary = "故事公开目标下拉")
    @GetMapping("/ts-story-publics/story-options")
    public Result<Page<TsStoryPublicTargetOptionVo>> pageStoryOptions(
            @RequestParam("ownerUserId") String ownerUserId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        return tsStoryPublicService.pageStoryOptions(currentUser(), ownerUserId, keyword, pageNo, pageSize);
    }

    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
