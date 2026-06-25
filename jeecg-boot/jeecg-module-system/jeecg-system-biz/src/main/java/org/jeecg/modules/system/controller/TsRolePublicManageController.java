package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicActionDto;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicQueryDto;
import org.jeecg.modules.system.dto.tsrolepublic.TsRolePublicSaveDto;
import org.jeecg.modules.system.service.ITsRolePublicService;
import org.jeecg.modules.system.vo.tsrolepublic.TsRolePublicTargetOptionVo;
import org.jeecg.modules.system.vo.tsrolepublic.TsRolePublicVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 角色公开记录管理接口。
 */
@Slf4j
@Tag(name = "TsRolePublic 角色公开记录")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsRolePublicManageController {

    private final ITsRolePublicService tsRolePublicService;

    public TsRolePublicManageController(ITsRolePublicService tsRolePublicService) {
        this.tsRolePublicService = tsRolePublicService;
    }

    @Operation(summary = "角色公开记录分页查询")
    @GetMapping("/ts-role-publics")
    public Result<Page<TsRolePublicVo>> listPublics(TsRolePublicQueryDto request) {
        return tsRolePublicService.pagePublics(currentUser(), request);
    }

    @Operation(summary = "角色公开记录详情")
    @GetMapping("/ts-role-publics/detail")
    public Result<TsRolePublicVo> getPublic(@RequestParam("id") Long id) {
        return tsRolePublicService.getPublic(currentUser(), id);
    }

    @Operation(summary = "新增角色公开记录")
    @PostMapping("/ts-role-publics")
    public Result<TsRolePublicVo> createPublic(
            @Validated(TsRolePublicSaveDto.Create.class) @RequestBody TsRolePublicSaveDto request) {
        return tsRolePublicService.addPublic(currentUser(), request);
    }

    @Operation(summary = "编辑角色公开记录")
    @PutMapping("/ts-role-publics")
    public Result<TsRolePublicVo> updatePublic(
            @Validated(TsRolePublicSaveDto.Update.class) @RequestBody TsRolePublicSaveDto request) {
        return tsRolePublicService.editPublic(currentUser(), request.getId(), request);
    }

    @Operation(summary = "删除角色公开记录")
    @DeleteMapping("/ts-role-publics")
    public Result<?> deletePublic(@RequestParam("id") Long id) {
        return tsRolePublicService.deletePublic(currentUser(), id);
    }

    @Operation(summary = "提交角色公开记录")
    @PostMapping("/ts-role-publics/submit")
    public Result<TsRolePublicVo> submit(@Validated @RequestBody TsRolePublicActionDto request) {
        return tsRolePublicService.submitPublic(currentUser(), request);
    }

    @Operation(summary = "审核通过角色公开记录")
    @PostMapping("/ts-role-publics/approve")
    public Result<TsRolePublicVo> approve(@Validated @RequestBody TsRolePublicActionDto request) {
        return tsRolePublicService.approvePublic(currentUser(), request);
    }

    @Operation(summary = "驳回角色公开记录")
    @PostMapping("/ts-role-publics/reject")
    public Result<TsRolePublicVo> reject(@Validated @RequestBody TsRolePublicActionDto request) {
        return tsRolePublicService.rejectPublic(currentUser(), request);
    }

    @Operation(summary = "上架角色公开记录")
    @PostMapping("/ts-role-publics/online")
    public Result<TsRolePublicVo> online(@Validated @RequestBody TsRolePublicActionDto request) {
        return tsRolePublicService.onlinePublic(currentUser(), request);
    }

    @Operation(summary = "下架角色公开记录")
    @PostMapping("/ts-role-publics/offline")
    public Result<TsRolePublicVo> offline(@Validated @RequestBody TsRolePublicActionDto request) {
        return tsRolePublicService.offlinePublic(currentUser(), request);
    }

    @Operation(summary = "角色公开目标下拉")
    @GetMapping("/ts-role-publics/role-options")
    public Result<Page<TsRolePublicTargetOptionVo>> pageRoleOptions(
            @RequestParam("ownerUserId") String ownerUserId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "pageNo", required = false, defaultValue = "1") Integer pageNo,
            @RequestParam(name = "pageSize", required = false, defaultValue = "20") Integer pageSize) {
        return tsRolePublicService.pageRoleOptions(currentUser(), ownerUserId, keyword, pageNo, pageSize);
    }

    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
