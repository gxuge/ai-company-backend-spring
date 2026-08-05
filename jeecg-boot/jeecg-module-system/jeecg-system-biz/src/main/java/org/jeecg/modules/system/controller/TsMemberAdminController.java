package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminConfigSaveDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminDeleteDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminIdDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminMembershipQueryDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminMembershipSaveDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminQuotaSaveDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsPaymentAdminQueryDto;
import org.jeecg.modules.system.service.ITsMemberAdminService;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminConfigVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminMembershipDetailVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminMembershipVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsPaymentAdminDetailVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsPaymentAdminVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 会员后台配置与用户会员管理接口。 */
@Tag(name = "TsMemberAdmin 会员后台管理")
@RestController
@Validated
@RequiresRoles("admin")
@RequestMapping("/sys/ts-member-admin")
public class TsMemberAdminController {

    @Autowired
    private ITsMemberAdminService memberAdminService;

    /** 查询全部会员配置。 */
    @Operation(summary = "查询会员后台配置")
    @GetMapping("/config")
    public Result<TsMemberAdminConfigVo> getConfig() {
        return Result.OK(memberAdminService.getConfig());
    }

    /** 新增或编辑会员配置。 */
    @Operation(summary = "保存会员配置")
    @PostMapping("/config/save")
    public Result<?> saveConfig(@Validated @RequestBody TsMemberAdminConfigSaveDto request) {
        memberAdminService.saveConfig(request);
        return Result.OK("保存成功");
    }

    /** 删除会员配置。 */
    @Operation(summary = "删除会员配置")
    @PostMapping("/config/delete")
    public Result<?> deleteConfig(@Validated @RequestBody TsMemberAdminDeleteDto request) {
        memberAdminService.deleteConfig(request);
        return Result.OK("删除成功");
    }

    /** 分页查询用户会员。 */
    @Operation(summary = "分页查询用户会员")
    @PostMapping("/membership/page")
    public Result<Page<TsMemberAdminMembershipVo>> pageMemberships(
            @RequestBody TsMemberAdminMembershipQueryDto request) {
        return Result.OK(memberAdminService.pageMemberships(request));
    }

    /** 新增或编辑用户会员。 */
    @Operation(summary = "保存用户会员")
    @PostMapping("/membership/save")
    public Result<?> saveMembership(
            @Validated @RequestBody TsMemberAdminMembershipSaveDto request) {
        memberAdminService.saveMembership(request);
        return Result.OK("保存成功");
    }

    /** 删除用户会员。 */
    @Operation(summary = "删除用户会员")
    @PostMapping("/membership/delete")
    public Result<?> deleteMembership(@Validated @RequestBody TsMemberAdminIdDto request) {
        memberAdminService.deleteMembership(request);
        return Result.OK("删除成功");
    }

    /** 查询用户会员详情。 */
    @Operation(summary = "查询用户会员详情")
    @PostMapping("/membership/detail")
    public Result<TsMemberAdminMembershipDetailVo> getMembershipDetail(
            @Validated @RequestBody TsMemberAdminIdDto request) {
        return Result.OK(memberAdminService.getMembershipDetail(request));
    }

    /** 新增或编辑用户权益额度。 */
    @Operation(summary = "保存用户权益额度")
    @PostMapping("/quota/save")
    public Result<?> saveQuota(@Validated @RequestBody TsMemberAdminQuotaSaveDto request) {
        memberAdminService.saveQuota(request);
        return Result.OK("保存成功");
    }

    /** 删除用户权益额度。 */
    @Operation(summary = "删除用户权益额度")
    @PostMapping("/quota/delete")
    public Result<?> deleteQuota(@Validated @RequestBody TsMemberAdminIdDto request) {
        memberAdminService.deleteQuota(request);
        return Result.OK("删除成功");
    }

    /** 分页查询支付流水。 */
    @Operation(summary = "分页查询支付流水")
    @PostMapping("/payment/page")
    public Result<Page<TsPaymentAdminVo>> pagePayments(
            @RequestBody TsPaymentAdminQueryDto request) {
        return Result.OK(memberAdminService.pagePayments(request));
    }

    /** 查询支付流水详情。 */
    @Operation(summary = "查询支付流水详情")
    @PostMapping("/payment/detail")
    public Result<TsPaymentAdminDetailVo> getPaymentDetail(
            @Validated @RequestBody TsMemberAdminIdDto request) {
        return Result.OK(memberAdminService.getPaymentDetail(request));
    }
}
