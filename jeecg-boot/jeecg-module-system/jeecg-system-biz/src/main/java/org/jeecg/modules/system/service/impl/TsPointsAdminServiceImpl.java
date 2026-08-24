package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.system.dto.tspoints.TsMemberPointsGiftRuleSaveDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdjustDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdminAccountQueryDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdminTransactionQueryDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsProductSaveDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRechargeAdminQueryDto;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.entity.TsMemberPointsGiftRule;
import org.jeecg.modules.system.entity.TsMemberProduct;
import org.jeecg.modules.system.entity.TsPointsRechargeProduct;
import org.jeecg.modules.system.mapper.SysUserMapper;
import org.jeecg.modules.system.mapper.TsMemberPlanMapper;
import org.jeecg.modules.system.mapper.TsMemberPointsGiftRuleMapper;
import org.jeecg.modules.system.mapper.TsMemberProductMapper;
import org.jeecg.modules.system.mapper.TsPointsQueryMapper;
import org.jeecg.modules.system.mapper.TsPointsRechargeProductMapper;
import org.jeecg.modules.system.mapper.TsPointsRechargeQueryMapper;
import org.jeecg.modules.system.service.ITsPointsAdminService;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.vo.tspoints.TsPointsAdminAccountVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsRechargeVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** 积分后台管理服务实现。 */
@Service
public class TsPointsAdminServiceImpl implements ITsPointsAdminService {

    private final TsPointsQueryMapper pointsQueryMapper;
    private final TsPointsRechargeQueryMapper rechargeQueryMapper;
    private final TsPointsRechargeProductMapper productMapper;
    private final TsMemberPointsGiftRuleMapper giftRuleMapper;
    private final TsMemberPlanMapper memberPlanMapper;
    private final TsMemberProductMapper memberProductMapper;
    private final SysUserMapper sysUserMapper;
    private final ITsPointsService pointsService;

    /** 注入后台积分管理依赖。 */
    public TsPointsAdminServiceImpl(
            TsPointsQueryMapper pointsQueryMapper,
            TsPointsRechargeQueryMapper rechargeQueryMapper,
            TsPointsRechargeProductMapper productMapper,
            TsMemberPointsGiftRuleMapper giftRuleMapper,
            TsMemberPlanMapper memberPlanMapper,
            TsMemberProductMapper memberProductMapper,
            SysUserMapper sysUserMapper,
            ITsPointsService pointsService) {
        this.pointsQueryMapper = pointsQueryMapper;
        this.rechargeQueryMapper = rechargeQueryMapper;
        this.productMapper = productMapper;
        this.giftRuleMapper = giftRuleMapper;
        this.memberPlanMapper = memberPlanMapper;
        this.memberProductMapper = memberProductMapper;
        this.sysUserMapper = sysUserMapper;
        this.pointsService = pointsService;
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsPointsAdminAccountVo> pageAccounts(
            TsPointsAdminAccountQueryDto request) {
        return pointsQueryMapper.selectAdminAccountPage(
                new Page<>(pageNo(request.getPageNo()), pageSize(request.getPageSize())), request);
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsPointsTransactionVo> pageTransactions(
            TsPointsAdminTransactionQueryDto request) {
        return pointsQueryMapper.selectAdminTransactionPage(
                new Page<>(pageNo(request.getPageNo()), pageSize(request.getPageSize())), request);
    }

    /** {@inheritDoc} */
    @Override
    public TsPointsTransactionVo adjust(
            TsPointsAdjustDto request, String operatorId) {
        SysUser user = sysUserMapper.selectById(request.getUserId());
        if (user == null) {
            throw new JeecgBootBizTipException("用户不存在");
        }
        return pointsService.adjust(request, operatorId);
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsPointsRechargeVo> pageRechargeOrders(
            TsPointsRechargeAdminQueryDto request) {
        return rechargeQueryMapper.selectAdminOrderPage(
                new Page<>(pageNo(request.getPageNo()), pageSize(request.getPageSize())), request);
    }

    /** {@inheritDoc} */
    @Override
    public List<TsPointsRechargeProduct> listProducts() {
        return productMapper.selectList(new LambdaQueryWrapper<TsPointsRechargeProduct>()
                .orderByAsc(TsPointsRechargeProduct::getSort)
                .orderByAsc(TsPointsRechargeProduct::getId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProduct(TsPointsProductSaveDto request) {
        if (request.getOriginalAmount().compareTo(request.getActualAmount()) < 0) {
            throw new JeecgBootBizTipException("积分商品实付金额不能高于原价");
        }
        Date now = new Date();
        TsPointsRechargeProduct product = new TsPointsRechargeProduct()
                .setId(request.getId())
                .setName(request.getName().trim())
                .setPoints(request.getPoints())
                .setGiftPoints(request.getGiftPoints())
                .setOriginalAmount(request.getOriginalAmount())
                .setActualAmount(request.getActualAmount())
                .setCurrency(request.getCurrency().trim().toUpperCase(Locale.ROOT))
                .setStatus(request.getStatus())
                .setSort(request.getSort() == null ? 0 : request.getSort())
                .setUpdatedAt(now);
        if (product.getId() == null) {
            product.setCreatedAt(now);
            productMapper.insert(product);
        } else if (productMapper.updateById(product) == 0) {
            throw new JeecgBootBizTipException("积分商品不存在");
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<TsMemberPointsGiftRule> listGiftRules() {
        return giftRuleMapper.selectList(new LambdaQueryWrapper<TsMemberPointsGiftRule>()
                .orderByAsc(TsMemberPointsGiftRule::getPlanId)
                .orderByAsc(TsMemberPointsGiftRule::getProductId)
                .orderByAsc(TsMemberPointsGiftRule::getId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveGiftRule(TsMemberPointsGiftRuleSaveDto request) {
        if (memberPlanMapper.selectById(request.getPlanId()) == null) {
            throw new JeecgBootBizTipException("会员等级不存在");
        }
        Long productId = request.getProductId() == null ? 0L : request.getProductId();
        if (productId > 0) {
            TsMemberProduct product = memberProductMapper.selectById(productId);
            if (product == null || !Objects.equals(product.getPlanId(), request.getPlanId())) {
                throw new JeecgBootBizTipException("会员套餐与会员等级不匹配");
            }
        }
        Date now = new Date();
        TsMemberPointsGiftRule rule = new TsMemberPointsGiftRule()
                .setId(request.getId())
                .setPlanId(request.getPlanId())
                .setProductId(productId)
                .setGiftPoints(request.getGiftPoints())
                .setStatus(request.getStatus())
                .setUpdatedAt(now);
        if (rule.getId() == null) {
            rule.setCreatedAt(now);
            giftRuleMapper.insert(rule);
        } else if (giftRuleMapper.updateById(rule) == 0) {
            throw new JeecgBootBizTipException("会员积分赠送规则不存在");
        }
    }

    /** 归一化页码。 */
    private int pageNo(Integer value) {
        return value == null ? 1 : Math.max(value, 1);
    }

    /** 归一化分页大小。 */
    private int pageSize(Integer value) {
        return value == null ? 10 : Math.min(Math.max(value, 1), 100);
    }
}
