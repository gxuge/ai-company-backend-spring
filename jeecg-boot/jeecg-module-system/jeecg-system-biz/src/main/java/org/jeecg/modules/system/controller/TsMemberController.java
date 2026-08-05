package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.service.ITsMemberService;
import org.jeecg.modules.system.vo.tsmember.TsMemberCompareVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberCurrentVo;
import org.jeecg.modules.system.vo.tsmember.TsMemberPageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员配置与当前状态接口。
 */
@Tag(name = "TsMember 会员订阅")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-members")
public class TsMemberController {

    @Autowired
    private ITsMemberService tsMemberService;

    /**
     * 获取会员首页配置。
     */
    @Operation(summary = "获取会员首页配置")
    @GetMapping("/page")
    public Result<TsMemberPageVo> getMemberPage() {
        return Result.OK(tsMemberService.getMemberPage());
    }

    /**
     * 获取 PRO 与 ULTRA 权益对比。
     */
    @Operation(summary = "获取会员权益对比")
    @GetMapping("/compare")
    public Result<TsMemberCompareVo> getMemberCompare() {
        return Result.OK(tsMemberService.getMemberCompare());
    }

    /**
     * 查询当前登录用户会员状态。
     */
    @Operation(summary = "查询当前用户会员状态")
    @GetMapping("/current")
    public Result<TsMemberCurrentVo> getCurrentMembership() {
        return Result.OK(tsMemberService.getCurrentMembership(currentUser()));
    }

    /**
     * 获取当前 Shiro 登录用户。
     */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
