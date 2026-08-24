package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tspoints.TsPointsAdjustDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsChangeDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRefundDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsTransactionQueryDto;
import org.jeecg.modules.system.entity.TsPointsTransaction;
import org.jeecg.modules.system.entity.TsUserPointsAccount;
import org.jeecg.modules.system.enums.tspoints.TsPointsBizType;
import org.jeecg.modules.system.enums.tspoints.TsPointsDirection;
import org.jeecg.modules.system.enums.tspoints.TsPointsErrorCode;
import org.jeecg.modules.system.enums.tspoints.TsPointsTransactionStatus;
import org.jeecg.modules.system.exception.tspoints.TsPointsBizException;
import org.jeecg.modules.system.mapper.TsPointsQueryMapper;
import org.jeecg.modules.system.mapper.TsPointsTransactionMapper;
import org.jeecg.modules.system.service.ITsPointsService;
import org.jeecg.modules.system.vo.tspoints.TsPointsAccountVo;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** 积分统一记账服务实现。 */
@Service
public class TsPointsServiceImpl implements ITsPointsService {

    private static final Set<TsPointsBizType> INCOME_TYPES = Set.of(
            TsPointsBizType.RECHARGE,
            TsPointsBizType.MEMBER_GIFT,
            TsPointsBizType.ACTIVITY_REWARD,
            TsPointsBizType.SIGN_IN,
            TsPointsBizType.COMPENSATION);
    private static final Set<TsPointsBizType> EXPENSE_TYPES = Set.of(
            TsPointsBizType.AI_CHAT,
            TsPointsBizType.IMAGE_GENERATE,
            TsPointsBizType.VOICE_GENERATE,
            TsPointsBizType.STORY_GENERATE,
            TsPointsBizType.ROLE_CREATE,
            TsPointsBizType.THREE_D_GENERATE,
            TsPointsBizType.ADVANCED_FEATURE);

    private final TsPointsQueryMapper queryMapper;
    private final TsPointsTransactionMapper transactionMapper;

    /** 注入积分查询与流水 Mapper。 */
    public TsPointsServiceImpl(
            TsPointsQueryMapper queryMapper,
            TsPointsTransactionMapper transactionMapper) {
        this.queryMapper = queryMapper;
        this.transactionMapper = transactionMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsPointsAccountVo getAccount(String userId) {
        requireUserId(userId);
        queryMapper.insertAccountIgnore(userId);
        return toAccountVo(requireAccount(queryMapper.selectAccount(userId)));
    }

    /** {@inheritDoc} */
    @Override
    public Page<TsPointsTransactionVo> pageTransactions(
            String userId, TsPointsTransactionQueryDto request) {
        requireUserId(userId);
        int pageNo = normalizePageNo(request.getPageNo());
        int pageSize = normalizePageSize(request.getPageSize());
        normalizeOptionalEnum(request.getDirection(), TsPointsDirection.class, "积分方向不合法");
        normalizeOptionalEnum(request.getBizType(), TsPointsBizType.class, "积分业务类型不合法");
        if (StringUtils.hasText(request.getDirection())) {
            request.setDirection(request.getDirection().trim().toUpperCase());
        }
        if (StringUtils.hasText(request.getBizType())) {
            request.setBizType(request.getBizType().trim().toUpperCase());
        }
        return queryMapper.selectUserTransactionPage(
                new Page<>(pageNo, pageSize), userId, request);
    }

    /** {@inheritDoc} */
    @Override
    public TsPointsTransactionVo getTransaction(String userId, Long id) {
        requireUserId(userId);
        TsPointsTransactionVo transaction = queryMapper.selectUserTransactionDetail(userId, id);
        if (transaction == null) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_TRANSACTION_NOT_FOUND, "积分流水不存在");
        }
        return transaction;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsPointsTransactionVo add(TsPointsChangeDto request) {
        TsPointsBizType bizType = requireBizType(request.getBizType());
        if (!INCOME_TYPES.contains(bizType)) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_BIZ_TYPE_INVALID,
                    "该积分业务类型不允许执行收入记账");
        }
        return applyChange(
                request.getUserId(),
                request.getAmount(),
                TsPointsDirection.INCOME,
                bizType,
                request.getBizId(),
                request.getDescription(),
                request.getIdempotencyKey(),
                null,
                null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsPointsTransactionVo consume(TsPointsChangeDto request) {
        TsPointsBizType bizType = requireBizType(request.getBizType());
        if (!EXPENSE_TYPES.contains(bizType)) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_BIZ_TYPE_INVALID,
                    "该积分业务类型不允许执行支出记账");
        }
        return applyChange(
                request.getUserId(),
                request.getAmount(),
                TsPointsDirection.EXPENSE,
                bizType,
                request.getBizId(),
                request.getDescription(),
                request.getIdempotencyKey(),
                null,
                null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsPointsTransactionVo refund(TsPointsRefundDto request) {
        requireAmount(request.getAmount());
        if (!StringUtils.hasText(request.getIdempotencyKey())) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_DUPLICATE_REQUEST, "积分幂等Key不能为空");
        }
        String idempotencyKey = request.getIdempotencyKey().trim();
        TsPointsTransaction original = queryMapper.selectTransactionForUpdate(
                request.getUserId(), request.getOriginalTransactionNo());
        if (original == null
                || !TsPointsDirection.EXPENSE.name().equals(original.getDirection())
                || !TsPointsTransactionStatus.SUCCESS.name().equals(original.getStatus())) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_TRANSACTION_NOT_FOUND,
                    "原消费积分流水不存在");
        }
        TsPointsTransaction existing = queryMapper.selectByIdempotencyKey(
                request.getUserId(), idempotencyKey);
        if (existing != null) {
            verifyIdempotentRequest(
                    existing,
                    TsPointsDirection.INCOME,
                    TsPointsBizType.REFUND,
                    request.getAmount());
            return toTransactionVo(existing);
        }
        long refunded = safeLong(queryMapper.sumSuccessfulRefunds(original.getTransactionNo()));
        if (refunded + request.getAmount() > original.getAmount()) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_REFUND_EXCEEDED,
                    "累计返还积分不能超过原消费数量",
                    false,
                    Map.of(
                            "originalAmount", original.getAmount(),
                            "refundedAmount", refunded,
                            "requestAmount", request.getAmount()));
        }
        return applyChange(
                request.getUserId(),
                request.getAmount(),
                TsPointsDirection.INCOME,
                TsPointsBizType.REFUND,
                request.getBizId(),
                request.getReason(),
                idempotencyKey,
                original.getTransactionNo(),
                null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TsPointsTransactionVo adjust(
            TsPointsAdjustDto request, String operatorId) {
        String operation = request.getOperation() == null
                ? "" : request.getOperation().trim().toUpperCase();
        TsPointsDirection direction;
        TsPointsBizType bizType;
        if ("ADD".equals(operation)) {
            direction = TsPointsDirection.INCOME;
            bizType = TsPointsBizType.ADMIN_ADD;
        } else if ("DEDUCT".equals(operation)) {
            direction = TsPointsDirection.EXPENSE;
            bizType = TsPointsBizType.ADMIN_DEDUCT;
        } else {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_INVALID_AMOUNT,
                    "积分调整操作仅支持ADD或DEDUCT");
        }
        String idempotencyKey = StringUtils.hasText(request.getIdempotencyKey())
                ? request.getIdempotencyKey().trim()
                : "ADMIN:" + operatorId + ":" + UUID.randomUUID();
        return applyChange(
                request.getUserId(),
                request.getAmount(),
                direction,
                bizType,
                null,
                request.getReason(),
                idempotencyKey,
                null,
                operatorId);
    }

    /**
     * 锁定用户账户后检查幂等、原子修改余额并写入成功流水。
     */
    private TsPointsTransactionVo applyChange(
            String userId,
            Long amount,
            TsPointsDirection direction,
            TsPointsBizType bizType,
            String bizId,
            String description,
            String idempotencyKey,
            String originalTransactionNo,
            String operatorId) {
        requireUserId(userId);
        requireAmount(amount);
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_DUPLICATE_REQUEST, "积分幂等Key不能为空");
        }

        queryMapper.insertAccountIgnore(userId);
        TsUserPointsAccount account = requireAccount(queryMapper.selectAccountForUpdate(userId));
        TsPointsTransaction existing = queryMapper.selectByIdempotencyKey(
                userId, idempotencyKey.trim());
        if (existing != null) {
            verifyIdempotentRequest(existing, direction, bizType, amount);
            return toTransactionVo(existing);
        }

        long beforeBalance = safeLong(account.getBalance());
        int updated;
        long afterBalance;
        if (direction == TsPointsDirection.INCOME) {
            updated = queryMapper.increaseBalance(account.getId(), amount);
            afterBalance = beforeBalance + amount;
        } else {
            updated = queryMapper.deductBalance(account.getId(), amount);
            afterBalance = beforeBalance - amount;
        }
        if (updated != 1 || afterBalance < 0) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_NOT_ENOUGH,
                    "积分余额不足",
                    false,
                    Map.of("required", amount, "balance", beforeBalance));
        }

        TsPointsTransaction transaction = new TsPointsTransaction()
                .setTransactionNo(buildTransactionNo())
                .setUserId(userId)
                .setDirection(direction.name())
                .setBizType(bizType.name())
                .setBizId(normalizeText(bizId))
                .setAmount(amount)
                .setBeforeBalance(beforeBalance)
                .setAfterBalance(afterBalance)
                .setStatus(TsPointsTransactionStatus.SUCCESS.name())
                .setDescription(normalizeText(description))
                .setIdempotencyKey(idempotencyKey.trim())
                .setOriginalTransactionNo(originalTransactionNo)
                .setOperatorId(operatorId)
                .setCreatedAt(new Date());
        transactionMapper.insert(transaction);
        return toTransactionVo(transaction);
    }

    /** 校验重复幂等 Key 的业务语义与原请求一致。 */
    private void verifyIdempotentRequest(
            TsPointsTransaction existing,
            TsPointsDirection direction,
            TsPointsBizType bizType,
            Long amount) {
        if (!Objects.equals(existing.getDirection(), direction.name())
                || !Objects.equals(existing.getBizType(), bizType.name())
                || !Objects.equals(existing.getAmount(), amount)) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_DUPLICATE_REQUEST,
                    "幂等Key已被其他积分请求使用");
        }
    }

    /** 校验并转换积分业务类型。 */
    private TsPointsBizType requireBizType(String value) {
        try {
            return TsPointsBizType.valueOf(value == null ? "" : value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_BIZ_TYPE_INVALID, "积分业务类型不合法");
        }
    }

    /** 校验积分数量。 */
    private void requireAmount(Long amount) {
        if (amount == null || amount <= 0) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_INVALID_AMOUNT, "积分数量必须大于0");
        }
    }

    /** 校验用户ID。 */
    private void requireUserId(String userId) {
        if (!StringUtils.hasText(userId)) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_ACCOUNT_NOT_FOUND, "用户ID不能为空");
        }
    }

    /** 校验账户存在。 */
    private TsUserPointsAccount requireAccount(TsUserPointsAccount account) {
        if (account == null) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_ACCOUNT_NOT_FOUND, "积分账户不存在");
        }
        return account;
    }

    /** 校验可选枚举值。 */
    private <E extends Enum<E>> void normalizeOptionalEnum(
            String value, Class<E> enumType, String message) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        try {
            Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new TsPointsBizException(
                    TsPointsErrorCode.POINTS_INVALID_AMOUNT, message);
        }
    }

    /** 转换积分账户响应。 */
    private TsPointsAccountVo toAccountVo(TsUserPointsAccount account) {
        TsPointsAccountVo vo = new TsPointsAccountVo();
        vo.setUserId(account.getUserId());
        vo.setBalance(safeLong(account.getBalance()));
        vo.setTotalIncome(safeLong(account.getTotalIncome()));
        vo.setTotalExpense(safeLong(account.getTotalExpense()));
        return vo;
    }

    /** 转换积分流水响应。 */
    private TsPointsTransactionVo toTransactionVo(TsPointsTransaction transaction) {
        TsPointsTransactionVo vo = new TsPointsTransactionVo();
        vo.setTransactionId(transaction.getId());
        vo.setTransactionNo(transaction.getTransactionNo());
        vo.setTitle(transaction.getDescription());
        vo.setUserId(transaction.getUserId());
        vo.setBizType(transaction.getBizType());
        vo.setBizId(transaction.getBizId());
        vo.setDirection(transaction.getDirection());
        vo.setAmount(transaction.getAmount());
        vo.setBeforeBalance(transaction.getBeforeBalance());
        vo.setAfterBalance(transaction.getAfterBalance());
        vo.setStatus(transaction.getStatus());
        vo.setDescription(transaction.getDescription());
        vo.setOriginalTransactionNo(transaction.getOriginalTransactionNo());
        vo.setOperatorId(transaction.getOperatorId());
        vo.setCreatedAt(transaction.getCreatedAt());
        return vo;
    }

    /** 生成全局积分流水号。 */
    private String buildTransactionNo() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date());
        String random = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 10).toUpperCase();
        return "PTS" + timestamp + random;
    }

    /** 标准化可选文本。 */
    private String normalizeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /** 读取可空 Long。 */
    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    /** 归一化页码。 */
    private int normalizePageNo(Integer pageNo) {
        return pageNo == null ? 1 : Math.max(pageNo, 1);
    }

    /** 归一化分页大小。 */
    private int normalizePageSize(Integer pageSize) {
        return pageSize == null ? 10 : Math.min(Math.max(pageSize, 1), 100);
    }
}
