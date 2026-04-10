package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsrole.TsRoleGenerateRoleDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickImageGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickSettingGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleOneClickVoiceGenerateDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleQueryDto;
import org.jeecg.modules.system.dto.tsrole.TsRoleSaveDto;
import org.jeecg.modules.system.service.ITsRoleService;
import org.jeecg.modules.system.vo.tsrole.TsRoleGenerateRoleVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickImageGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickSettingGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleOneClickVoiceGenerateVo;
import org.jeecg.modules.system.vo.tsrole.TsRoleVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@Slf4j
@Tag(name = "TsRole 角色核心")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsRoleController {

    @Autowired
    private ITsRoleService tsRoleService;
    @Operation(summary = "角色分页查询")
    @GetMapping("/ts-roles")
    public Result<Page<TsRoleVo>> listRoles(TsRoleQueryDto request) {
        return tsRoleService.pageRoles(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "角色详情")
    @GetMapping("/ts-roles/detail")
    public Result<TsRoleVo> getRole(@RequestParam("id") Long id) {
        return tsRoleService.getRole(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "新增角色")
    @PostMapping("/ts-roles")
    public Result<TsRoleVo> createRole(@Validated(TsRoleSaveDto.Create.class) @RequestBody TsRoleSaveDto request) {
        return tsRoleService.addRole(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "编辑角色")
    @PutMapping("/ts-roles")
    public Result<TsRoleVo> updateRole(@Validated(TsRoleSaveDto.Update.class) @RequestBody TsRoleSaveDto request) {
        return tsRoleService.editRole(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }
    @Operation(summary = "删除角色")
    @DeleteMapping("/ts-roles")
    public Result<?> removeRole(@RequestParam("id") Long id) {
        return tsRoleService.deleteRole(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "Role one-click setting generate")
    @PostMapping("/ts-roles/one-click-setting")
    public Result<TsRoleOneClickSettingGenerateVo> generateRoleSetting(@RequestBody TsRoleOneClickSettingGenerateDto request) {
        return tsRoleService.generateRoleSetting(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "Role one-click image generate")
    @PostMapping("/ts-roles/one-click-image")
    public Result<TsRoleOneClickImageGenerateVo> generateRoleImage(@RequestBody TsRoleOneClickImageGenerateDto request) {
        return tsRoleService.generateRoleImage(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "Role one-click voice generate")
    @PostMapping("/ts-roles/one-click-voice")
    public Result<TsRoleOneClickVoiceGenerateVo> generateRoleVoice(@RequestBody TsRoleOneClickVoiceGenerateDto request) {
        return tsRoleService.generateRoleVoice(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }

    @Operation(summary = "Role generate role")
    @PostMapping("/ts-roles/generate-role")
    public Result<TsRoleGenerateRoleVo> generateRole(@RequestBody TsRoleGenerateRoleDto request) {
        return tsRoleService.generateRole(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
}
