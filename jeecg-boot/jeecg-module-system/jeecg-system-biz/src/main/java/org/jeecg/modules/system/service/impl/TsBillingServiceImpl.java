package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.common.exception.JeecgBootBizTipException;
import org.jeecg.modules.system.dto.tsbilling.TsBillingQueryDto;
import org.jeecg.modules.system.mapper.TsBillingQueryMapper;
import org.jeecg.modules.system.service.ITsBillingService;
import org.jeecg.modules.system.vo.tsbilling.TsBillingDetailVo;
import org.jeecg.modules.system.vo.tsbilling.TsBillingRecordVo;
import org.jeecg.modules.system.vo.tsbilling.TsBillingSummaryVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

/** 双视角统一账单服务实现。 */
@Service
public class TsBillingServiceImpl implements ITsBillingService {

    private static final Set<String> RECORD_TYPES =
            Set.of("MEMBERSHIP", "RECHARGE", "POINTS");
    private static final Set<String> DIRECTIONS =
            Set.of("ALL", "INCOME", "EXPENSE", "NONE");

    private final TsBillingQueryMapper billingQueryMapper;

    /** 注入统一账单查询 Mapper。 */
    public TsBillingServiceImpl(TsBillingQueryMapper billingQueryMapper) {
        this.billingQueryMapper = billingQueryMapper;
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsBillingRecordVo> pageUserBills(
            String userId, TsBillingQueryDto request) {
        normalize(request);
        request.setKeyword(null);
        return billingQueryMapper.selectBillingPage(
                page(request), request, userId, false);
    }

    /** {@inheritDoc} */
    @Override
    public TsBillingDetailVo getUserBill(
            String userId, String recordType, Long recordId) {
        String normalizedType = requireRecordType(recordType);
        TsBillingDetailVo detail = billingQueryMapper.selectBillingDetail(
                normalizedType, recordId, userId, false);
        if (detail == null) {
            throw new JeecgBootBizTipException("账单不存在或无权限访问");
        }
        return detail;
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsBillingRecordVo> pagePlatformBills(TsBillingQueryDto request) {
        normalize(request);
        return billingQueryMapper.selectBillingPage(
                page(request), request, null, true);
    }

    /** {@inheritDoc} */
    @Override
    public TsBillingDetailVo getPlatformBill(String recordType, Long recordId) {
        String normalizedType = requireRecordType(recordType);
        TsBillingDetailVo detail = billingQueryMapper.selectBillingDetail(
                normalizedType, recordId, null, true);
        if (detail == null) {
            throw new JeecgBootBizTipException("账单不存在");
        }
        return detail;
    }

    /** {@inheritDoc} */
    @Override
    public TsBillingSummaryVo summarizePlatformBills(TsBillingQueryDto request) {
        normalize(request);
        return billingQueryMapper.selectPlatformSummary(request);
    }

    /** 校验并归一化账单查询条件。 */
    private void normalize(TsBillingQueryDto request) {
        request.setCategory(normalizeUpper(request.getCategory(), "ALL"));
        request.setMoneyDirection(normalizeUpper(request.getMoneyDirection(), "ALL"));
        request.setPointsDirection(normalizeUpper(request.getPointsDirection(), "ALL"));
        if (!"ALL".equals(request.getCategory())
                && !RECORD_TYPES.contains(request.getCategory())) {
            throw new JeecgBootBizTipException("账单分类不合法");
        }
        if (!DIRECTIONS.contains(request.getMoneyDirection())
                || !DIRECTIONS.contains(request.getPointsDirection())) {
            throw new JeecgBootBizTipException("账单方向不合法");
        }
        if (StringUtils.hasText(request.getBizType())) {
            request.setBizType(request.getBizType().trim().toUpperCase(Locale.ROOT));
        }
        if (StringUtils.hasText(request.getStatus())) {
            request.setStatus(request.getStatus().trim().toUpperCase(Locale.ROOT));
        }
    }

    /** 校验记录类型。 */
    private String requireRecordType(String recordType) {
        String normalized = normalizeUpper(recordType, "");
        if (!RECORD_TYPES.contains(normalized)) {
            throw new JeecgBootBizTipException("账单记录类型不合法");
        }
        return normalized;
    }

    /** 创建受限分页对象。 */
    private Page<TsBillingRecordVo> page(TsBillingQueryDto request) {
        int pageNo = request.getPageNo() == null
                ? 1 : Math.max(request.getPageNo(), 1);
        int pageSize = request.getPageSize() == null
                ? 10 : Math.min(Math.max(request.getPageSize(), 1), 100);
        return new Page<>(pageNo, pageSize);
    }

    /** 转换为大写枚举文本。 */
    private String normalizeUpper(String value, String defaultValue) {
        return StringUtils.hasText(value)
                ? value.trim().toUpperCase(Locale.ROOT) : defaultValue;
    }
}
