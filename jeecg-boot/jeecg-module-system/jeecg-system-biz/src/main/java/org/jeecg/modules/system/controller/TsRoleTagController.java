package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.service.ITsRoleTagService;
import org.jeecg.modules.system.vo.tsroletag.TsRoleTagVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@Tag(name = "TsRoleTag 角色标签")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys")
public class TsRoleTagController {

    @Autowired
    private ITsRoleTagService tsRoleTagService;

    @Operation(summary = "查询官方角色标签列表")
    @GetMapping("/ts-role-tags")
    public Result<List<TsRoleTagVo>> listRoleTags() {
        return tsRoleTagService.listRoleTags((LoginUser) SecurityUtils.getSubject().getPrincipal());
    }
}

