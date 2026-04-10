package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.mapper.TsRoleMapper;
import org.jeecg.modules.system.vo.tsrole.TsRoleAuthorPublicVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "TsRole public author")
@RestController
@Validated
@RequestMapping("/sys")
public class TsRolePublicController {

    @Autowired
    private TsRoleMapper tsRoleMapper;

    @Operation(summary = "Get role author public profile by roleId")
    @GetMapping("/ts-roles/author-public")
    public Result<TsRoleAuthorPublicVo> getRoleAuthorPublic(@RequestParam("roleId") Long roleId) {
        if (roleId == null) {
            return Result.error("roleId不能为空");
        }
        TsRoleAuthorPublicVo authorPublic = tsRoleMapper.selectRoleAuthorPublic(roleId);
        if (authorPublic == null) {
            return Result.error("角色不存在或未公开");
        }
        return Result.OK(authorPublic);
    }
}
