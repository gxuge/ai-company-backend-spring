package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminConfigSaveDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminDeleteDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminIdDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminMembershipQueryDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminMembershipSaveDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsMemberAdminQuotaSaveDto;
import org.jeecg.modules.system.dto.tsmemberadmin.TsPaymentAdminQueryDto;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.entity.TsMemberBenefit;
import org.jeecg.modules.system.entity.TsMemberGift;
import org.jeecg.modules.system.entity.TsMemberPlan;
import org.jeecg.modules.system.entity.TsMemberPlanBenefit;
import org.jeecg.modules.system.entity.TsMemberProduct;
import org.jeecg.modules.system.entity.TsUserBenefitQuota;
import org.jeecg.modules.system.entity.TsUserMembership;
import org.jeecg.modules.system.mapper.SysUserMapper;
import org.jeecg.modules.system.mapper.TsMemberBenefitMapper;
import org.jeecg.modules.system.mapper.TsMemberGiftMapper;
import org.jeecg.modules.system.mapper.TsMemberPlanBenefitMapper;
import org.jeecg.modules.system.mapper.TsMemberPlanMapper;
import org.jeecg.modules.system.mapper.TsMemberProductMapper;
import org.jeecg.modules.system.mapper.TsMemberQueryMapper;
import org.jeecg.modules.system.mapper.TsPaymentQueryMapper;
import org.jeecg.modules.system.mapper.TsUserBenefitQuotaMapper;
import org.jeecg.modules.system.mapper.TsUserMembershipMapper;
import org.jeecg.modules.system.service.ITsMemberAdminService;
import org.jeecg.modules.system.util.tspayment.TsPaymentResponseSanitizer;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminConfigVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminMembershipDetailVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsMemberAdminMembershipVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsPaymentAdminDetailVo;
import org.jeecg.modules.system.vo.tsmemberadmin.TsPaymentAdminVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;

/** 会员后台管理服务实现。 */
@Service
public class TsMemberAdminServiceImpl implements ITsMemberAdminService {

    @Autowired private ObjectMapper objectMapper;
    @Autowired private TsMemberPlanMapper planMapper;
    @Autowired private TsMemberProductMapper productMapper;
    @Autowired private TsMemberBenefitMapper benefitMapper;
    @Autowired private TsMemberPlanBenefitMapper planBenefitMapper;
    @Autowired private TsMemberGiftMapper giftMapper;
    @Autowired private TsUserMembershipMapper membershipMapper;
    @Autowired private TsUserBenefitQuotaMapper quotaMapper;
    @Autowired private TsMemberQueryMapper queryMapper;
    @Autowired private TsPaymentQueryMapper paymentQueryMapper;
    @Autowired private SysUserMapper sysUserMapper;

    /** 查询全部会员配置。 */
    @Override
    public TsMemberAdminConfigVo getConfig() {
        TsMemberAdminConfigVo vo = new TsMemberAdminConfigVo();
        vo.setPlans(planMapper.selectList(new LambdaQueryWrapper<TsMemberPlan>()
                .orderByAsc(TsMemberPlan::getSort).orderByAsc(TsMemberPlan::getId)));
        vo.setProducts(productMapper.selectList(new LambdaQueryWrapper<TsMemberProduct>()
                .orderByAsc(TsMemberProduct::getPlanId).orderByAsc(TsMemberProduct::getId)));
        vo.setBenefits(benefitMapper.selectList(new LambdaQueryWrapper<TsMemberBenefit>()
                .orderByAsc(TsMemberBenefit::getSort).orderByAsc(TsMemberBenefit::getId)));
        vo.setPlanBenefits(planBenefitMapper.selectList(
                new LambdaQueryWrapper<TsMemberPlanBenefit>()
                        .orderByAsc(TsMemberPlanBenefit::getPlanId)
                        .orderByAsc(TsMemberPlanBenefit::getId)));
        vo.setGifts(giftMapper.selectList(new LambdaQueryWrapper<TsMemberGift>()
                .orderByAsc(TsMemberGift::getPlanId)
                .orderByAsc(TsMemberGift::getSort)
                .orderByAsc(TsMemberGift::getId)));
        return vo;
    }

    /** 按资源类型保存会员配置。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveConfig(TsMemberAdminConfigSaveDto request) {
        switch (request.getResourceType()) {
            case "plan" -> saveEntity(objectMapper.convertValue(request.getData(), TsMemberPlan.class), planMapper);
            case "product" -> saveProduct(objectMapper.convertValue(request.getData(), TsMemberProduct.class));
            case "benefit" -> saveEntity(objectMapper.convertValue(request.getData(), TsMemberBenefit.class), benefitMapper);
            case "planBenefit" -> savePlanBenefit(objectMapper.convertValue(
                    request.getData(), TsMemberPlanBenefit.class));
            case "gift" -> saveEntity(objectMapper.convertValue(request.getData(), TsMemberGift.class), giftMapper);
            default -> throw new JeecgBootBizTipException("不支持的会员配置类型");
        }
    }

    /** 按资源类型删除会员配置，并保护仍被引用的数据。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(TsMemberAdminDeleteDto request) {
        Long id = request.getId();
        switch (request.getResourceType()) {
            case "plan" -> {
                boolean used = productMapper.selectCount(new LambdaQueryWrapper<TsMemberProduct>()
                        .eq(TsMemberProduct::getPlanId, id)) > 0
                        || membershipMapper.selectCount(new LambdaQueryWrapper<TsUserMembership>()
                        .eq(TsUserMembership::getPlanId, id)) > 0;
                if (used) {
                    throw new JeecgBootBizTipException("会员等级已被使用，请先停用");
                }
                planBenefitMapper.delete(new LambdaQueryWrapper<TsMemberPlanBenefit>()
                        .eq(TsMemberPlanBenefit::getPlanId, id));
                giftMapper.delete(new LambdaQueryWrapper<TsMemberGift>()
                        .eq(TsMemberGift::getPlanId, id));
                planMapper.deleteById(id);
            }
            case "product" -> {
                if (membershipMapper.selectCount(new LambdaQueryWrapper<TsUserMembership>()
                        .eq(TsUserMembership::getProductId, id)) > 0) {
                    throw new JeecgBootBizTipException("会员套餐已被使用，请先停用");
                }
                productMapper.deleteById(id);
            }
            case "benefit" -> {
                if (planBenefitMapper.selectCount(new LambdaQueryWrapper<TsMemberPlanBenefit>()
                        .eq(TsMemberPlanBenefit::getBenefitId, id)) > 0) {
                    throw new JeecgBootBizTipException("权益已关联会员等级，不能删除");
                }
                benefitMapper.deleteById(id);
            }
            case "planBenefit" -> planBenefitMapper.deleteById(id);
            case "gift" -> giftMapper.deleteById(id);
            default -> throw new JeecgBootBizTipException("不支持的会员配置类型");
        }
    }

    /** 分页查询用户会员。 */
    @Override
    public Page<TsMemberAdminMembershipVo> pageMemberships(
            TsMemberAdminMembershipQueryDto request) {
        int pageNo = request.getPageNo() == null ? 1 : Math.max(request.getPageNo(), 1);
        int pageSize = request.getPageSize() == null
                ? 10 : Math.min(Math.max(request.getPageSize(), 1), 100);
        return queryMapper.selectAdminMembershipPage(
                new Page<>(pageNo, pageSize), request);
    }

    /** 新增或更新用户会员。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMembership(TsMemberAdminMembershipSaveDto request) {
        SysUser user = sysUserMapper.selectById(request.getUserId());
        if (user == null) {
            throw new JeecgBootBizTipException("用户不存在");
        }
        TsMemberProduct product = productMapper.selectById(request.getProductId());
        if (product == null || !Objects.equals(product.getPlanId(), request.getPlanId())) {
            throw new JeecgBootBizTipException("会员套餐与会员等级不匹配");
        }
        if (!request.getEndTime().after(request.getStartTime())) {
            throw new JeecgBootBizTipException("到期时间必须晚于生效时间");
        }
        TsUserMembership entity = new TsUserMembership()
                .setId(request.getId())
                .setUserId(request.getUserId())
                .setPlanId(request.getPlanId())
                .setProductId(request.getProductId())
                .setStartTime(request.getStartTime())
                .setEndTime(request.getEndTime())
                .setStatus(request.getStatus())
                .setAutoRenew(request.getAutoRenew());
        if (entity.getId() == null) {
            entity.setCreatedAt(new Date());
            membershipMapper.insert(entity);
        } else if (membershipMapper.updateById(entity) == 0) {
            throw new JeecgBootBizTipException("用户会员记录不存在");
        }
    }

    /** 删除用户会员记录。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMembership(TsMemberAdminIdDto request) {
        membershipMapper.deleteById(request.getId());
    }

    /** 查询用户会员详情及关联记录。 */
    @Override
    public TsMemberAdminMembershipDetailVo getMembershipDetail(TsMemberAdminIdDto request) {
        TsMemberAdminMembershipVo membership = queryMapper.selectAdminMembershipById(request.getId());
        if (membership == null) {
            throw new JeecgBootBizTipException("用户会员记录不存在");
        }
        TsMemberAdminMembershipDetailVo vo = new TsMemberAdminMembershipDetailVo();
        vo.setMembership(membership);
        vo.setQuotas(quotaMapper.selectList(new LambdaQueryWrapper<TsUserBenefitQuota>()
                .eq(TsUserBenefitQuota::getUserId, membership.getUserId())
                .orderByAsc(TsUserBenefitQuota::getBenefitCode)));
        vo.setOrders(queryMapper.selectOrdersByUserId(membership.getUserId()));
        vo.setUsageLogs(queryMapper.selectUsageLogsByUserId(membership.getUserId()));
        return vo;
    }

    /** 新增或更新用户权益额度。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveQuota(TsMemberAdminQuotaSaveDto request) {
        if (request.getTotalAmount() >= 0 && request.getUsedAmount() > request.getTotalAmount()) {
            throw new JeecgBootBizTipException("已使用额度不能大于总额度");
        }
        TsUserBenefitQuota entity = new TsUserBenefitQuota()
                .setId(request.getId())
                .setUserId(request.getUserId())
                .setBenefitCode(request.getBenefitCode())
                .setTotalAmount(request.getTotalAmount())
                .setUsedAmount(request.getUsedAmount())
                .setExpireTime(request.getExpireTime());
        if (entity.getId() == null) {
            entity.setCreatedAt(new Date());
            quotaMapper.insert(entity);
        } else if (quotaMapper.updateById(entity) == 0) {
            throw new JeecgBootBizTipException("权益额度记录不存在");
        }
    }

    /** 删除用户权益额度。 */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteQuota(TsMemberAdminIdDto request) {
        quotaMapper.deleteById(request.getId());
    }

    /** 分页查询支付流水。 */
    @Override
    public Page<TsPaymentAdminVo> pagePayments(TsPaymentAdminQueryDto request) {
        int pageNo = request.getPageNo() == null ? 1 : Math.max(request.getPageNo(), 1);
        int pageSize = request.getPageSize() == null
                ? 10 : Math.min(Math.max(request.getPageSize(), 1), 100);
        return paymentQueryMapper.selectAdminPaymentPage(
                new Page<>(pageNo, pageSize), request);
    }

    /** 查询支付流水详情，并在返回前再次脱敏渠道响应。 */
    @Override
    public TsPaymentAdminDetailVo getPaymentDetail(TsMemberAdminIdDto request) {
        TsPaymentAdminDetailVo detail = paymentQueryMapper.selectAdminPaymentById(request.getId());
        if (detail == null) {
            throw new JeecgBootBizTipException("支付流水不存在");
        }
        detail.setRawResponse(TsPaymentResponseSanitizer.sanitize(
                detail.getRawResponse(), objectMapper));
        return detail;
    }

    /** 保存带 Long 主键的 MyBatis Plus 实体。 */
    private <T> void saveEntity(T entity, com.baomidou.mybatisplus.core.mapper.BaseMapper<T> mapper) {
        Long id = readId(entity);
        if (id == null) {
            mapper.insert(entity);
        } else if (mapper.updateById(entity) == 0) {
            throw new JeecgBootBizTipException("配置记录不存在");
        }
    }

    /** 保存套餐并校验会员等级。 */
    private void saveProduct(TsMemberProduct entity) {
        if (entity.getPlanId() == null || planMapper.selectById(entity.getPlanId()) == null) {
            throw new JeecgBootBizTipException("会员等级不存在");
        }
        saveEntity(entity, productMapper);
    }

    /** 保存等级权益并校验关联记录。 */
    private void savePlanBenefit(TsMemberPlanBenefit entity) {
        if (planMapper.selectById(entity.getPlanId()) == null
                || benefitMapper.selectById(entity.getBenefitId()) == null) {
            throw new JeecgBootBizTipException("会员等级或权益不存在");
        }
        saveEntity(entity, planBenefitMapper);
    }

    /** 读取配置实体 ID。 */
    private Long readId(Object entity) {
        if (entity instanceof TsMemberPlan value) return value.getId();
        if (entity instanceof TsMemberProduct value) return value.getId();
        if (entity instanceof TsMemberBenefit value) return value.getId();
        if (entity instanceof TsMemberPlanBenefit value) return value.getId();
        if (entity instanceof TsMemberGift value) return value.getId();
        throw new JeecgBootBizTipException("不支持的配置实体");
    }
}
