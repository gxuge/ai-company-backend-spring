package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileQueryDto;
import org.jeecg.modules.system.dto.tsroleimageprofile.TsRoleImageProfileSaveDto;
import org.jeecg.modules.system.service.ITsRoleImageProfileService;
import org.jeecg.modules.system.vo.tsroleimageprofile.TsRoleImageProfileVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
@Slf4j
@Tag(name = "TsRoleImageProfile 角色形象模板")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsRoleImageProfileController {

    @Autowired
    private ITsRoleImageProfileService tsRoleImageProfileService;
    @Operation(summary = "角色形象模板分页查询")
    @GetMapping("/ts-role-image-profiles")
    public Result<Page<TsRoleImageProfileVo>> listProfiles(TsRoleImageProfileQueryDto request) {
        return tsRoleImageProfileService.pageProfiles(((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "角色形象模板详情")
    @GetMapping("/ts-role-image-profiles/detail")
    public Result<TsRoleImageProfileVo> getProfile(@RequestParam("id") Long id) {
        return tsRoleImageProfileService.getProfile(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
    @Operation(summary = "新增角色形象模板")
    @PostMapping("/ts-role-image-profiles")
    public Result<TsRoleImageProfileVo> createProfile(
            @Validated(TsRoleImageProfileSaveDto.Create.class) @RequestBody TsRoleImageProfileSaveDto request) {
        return tsRoleImageProfileService.addProfile(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request);
    }
    @Operation(summary = "编辑角色形象模板")
    @PutMapping("/ts-role-image-profiles")
    public Result<TsRoleImageProfileVo> updateProfile(
            @Validated(TsRoleImageProfileSaveDto.Update.class) @RequestBody TsRoleImageProfileSaveDto request) {
        return tsRoleImageProfileService.editProfile(
                ((LoginUser) SecurityUtils.getSubject().getPrincipal()), request.getId(), request);
    }
    @Operation(summary = "删除角色形象模板")
    @DeleteMapping("/ts-role-image-profiles")
    public Result<?> removeProfile(@RequestParam("id") Long id) {
        return tsRoleImageProfileService.deleteProfile(((LoginUser) SecurityUtils.getSubject().getPrincipal()), id);
    }
}