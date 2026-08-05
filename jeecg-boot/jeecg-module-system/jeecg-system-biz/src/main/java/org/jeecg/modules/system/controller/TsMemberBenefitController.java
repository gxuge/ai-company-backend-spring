package org.jeecg.modules.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tsmember.TsMemberBenefitCheckDto;
import org.jeecg.modules.system.dto.tsmember.TsMemberBenefitConsumeDto;
import org.jeecg.modules.system.service.ITsMemberService;
import org.jeecg.modules.system.vo.tsmember.TsMemberBenefitCheckVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员权益检查与扣减接口。
 */
@Tag(name = "TsMemberBenefit 会员权益")
@RestController
@Validated
@RequiresAuthentication
@RequestMapping("/sys/ts-member-benefits")
public class TsMemberBenefitController {

    @Autowired
    private ITsMemberService tsMemberService;

    /**
     * 检查当前用户是否可使用指定权益。
     */
    @Operation(summary = "检查会员权益")
    @PostMapping("/check")
    public Result<TsMemberBenefitCheckVo> checkBenefit(
            @Validated @RequestBody TsMemberBenefitCheckDto request) {
        return Result.OK(tsMemberService.checkBenefit(currentUser(), request));
    }

    /**
     * 幂等消耗当前用户权益额度。
     */
    @Operation(summary = "消耗会员权益")
    @PostMapping("/consume")
    public Result<TsMemberBenefitCheckVo> consumeBenefit(
            @Validated @RequestBody TsMemberBenefitConsumeDto request) {
        return Result.OK("权益扣减成功",
                tsMemberService.consumeBenefit(currentUser(), request));
    }

    /**
     * 获取当前 Shiro 登录用户。
     */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
