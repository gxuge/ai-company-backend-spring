package org.jeecg.modules.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.modules.system.dto.tspoints.TsMemberPointsGiftRuleSaveDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdjustDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdminAccountQueryDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdminTransactionQueryDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsProductSaveDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeAdminQueryDto;
import org.jeecg.modules.system.entity.TsMemberPointsGiftRule;
import org.jeecg.modules.system.entity.TsPointsRechargeProduct;
import org.jeecg.modules.system.service.ITsPointsAdminService;
import org.jeecg.modules.system.vo.tspoints.TsPointsAdminAccountVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsRechargeVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 积分后台管理接口。 */
@Tag(name = "TsPointsAdmin 积分后台管理")
@RestController
@Validated
@RequiresRoles("admin")
@RequestMapping("/sys/ts-points-admin")
public class TsPointsAdminController {

    private final ITsPointsAdminService pointsAdminService;

    /** 注入积分后台服务。 */
    public TsPointsAdminController(ITsPointsAdminService pointsAdminService) {
        this.pointsAdminService = pointsAdminService;
    }

    /** 分页查询积分账户。 */
    @Operation(summary = "分页查询积分账户")
    @PostMapping("/account/page")
    public Result<Page<TsPointsAdminAccountVo>> pageAccounts(
            @RequestBody TsPointsAdminAccountQueryDto request) {
        return Result.OK(pointsAdminService.pageAccounts(request));
    }

    /** 分页查询积分流水。 */
    @Operation(summary = "分页查询积分流水")
    @PostMapping("/transaction/page")
    public Result<Page<TsPointsTransactionVo>> pageTransactions(
            @RequestBody TsPointsAdminTransactionQueryDto request) {
        return Result.OK(pointsAdminService.pageTransactions(request));
    }

    /** 后台手动调整积分。 */
    @Operation(summary = "后台手动调整积分")
    @PostMapping("/adjust")
    public Result<TsPointsTransactionVo> adjust(
            @Validated @RequestBody TsPointsAdjustDto request) {
        return Result.OK(pointsAdminService.adjust(request, currentUser().getId()));
    }

    /** 分页查询积分充值订单。 */
    @Operation(summary = "分页查询积分充值订单")
    @PostMapping("/recharge/page")
    public Result<Page<TsPointsRechargeVo>> pageRechargeOrders(
            @RequestBody TsPointsRechargeAdminQueryDto request) {
        return Result.OK(pointsAdminService.pageRechargeOrders(request));
    }

    /** 查询全部积分充值商品。 */
    @Operation(summary = "查询积分充值商品配置")
    @GetMapping("/product/list")
    public Result<List<TsPointsRechargeProduct>> listProducts() {
        return Result.OK(pointsAdminService.listProducts());
    }

    /** 保存积分充值商品。 */
    @Operation(summary = "保存积分充值商品")
    @PostMapping("/product/save")
    public Result<Void> saveProduct(
            @Validated @RequestBody TsPointsProductSaveDto request) {
        pointsAdminService.saveProduct(request);
        return Result.OK("保存成功");
    }

    /** 查询会员积分赠送规则。 */
    @Operation(summary = "查询会员积分赠送规则")
    @GetMapping("/member-gift-rule/list")
    public Result<List<TsMemberPointsGiftRule>> listGiftRules() {
        return Result.OK(pointsAdminService.listGiftRules());
    }

    /** 保存会员积分赠送规则。 */
    @Operation(summary = "保存会员积分赠送规则")
    @PostMapping("/member-gift-rule/save")
    public Result<Void> saveGiftRule(
            @Validated @RequestBody TsMemberPointsGiftRuleSaveDto request) {
        pointsAdminService.saveGiftRule(request);
        return Result.OK("保存成功");
    }

    /** 获取当前管理员。 */
    private LoginUser currentUser() {
        return (LoginUser) SecurityUtils.getSubject().getPrincipal();
    }
}
