package org.jeecg.modules.system.service.impl;

import org.jeecg.modules.system.dto.tspoints.TsPointsChangeDto;
import org.jeecg.modules.system.dto.tspoints.TsPointsRefundDto;
import org.jeecg.modules.system.entity.TsPointsTransaction;
import org.jeecg.modules.system.entity.TsUserPointsAccount;
import org.jeecg.modules.system.enums.tspoints.TsPointsErrorCode;
import org.jeecg.modules.system.exception.tspoints.TsPointsBizException;
import org.jeecg.modules.system.mapper.TsPointsQueryMapper;
import org.jeecg.modules.system.mapper.TsPointsTransactionMapper;
import org.jeecg.modules.system.vo.tspoints.TsPointsTransactionVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 积分统一记账服务测试。 */
class TsPointsServiceImplTest {

    private TsPointsQueryMapper queryMapper;
    private TsPointsTransactionMapper transactionMapper;
    private TsPointsServiceImpl service;

    /** 初始化积分服务依赖。 */
    @BeforeEach
    void setUp() {
        queryMapper = mock(TsPointsQueryMapper.class);
        transactionMapper = mock(TsPointsTransactionMapper.class);
        service = new TsPointsServiceImpl(queryMapper, transactionMapper);
    }

    /** 余额不足必须返回机器错误码且不得写流水。 */
    @Test
    void consumeShouldReturnMachineCodeWhenBalanceIsNotEnough() {
        TsUserPointsAccount account = new TsUserPointsAccount()
                .setId(1L)
                .setUserId("u1")
                .setBalance(20L);
        when(queryMapper.selectAccountForUpdate("u1")).thenReturn(account);
        when(queryMapper.deductBalance(1L, 100L)).thenReturn(0);

        TsPointsBizException exception = assertThrows(
                TsPointsBizException.class,
                () -> service.consume(change("u1", 100L, "AI_CHAT", "consume-1")));

        assertEquals(TsPointsErrorCode.POINTS_NOT_ENOUGH, exception.getErrorCode());
        assertEquals(20L, exception.getErrorArgs().get("balance"));
        verify(transactionMapper, never()).insert(any(TsPointsTransaction.class));
    }

    /** 重复增加请求必须直接返回原流水。 */
    @Test
    void addShouldReturnExistingTransactionForSameIdempotencyKey() {
        TsUserPointsAccount account = new TsUserPointsAccount()
                .setId(1L)
                .setUserId("u1")
                .setBalance(10L);
        TsPointsTransaction existing = new TsPointsTransaction()
                .setId(9L)
                .setTransactionNo("PTS1")
                .setUserId("u1")
                .setDirection("INCOME")
                .setBizType("SIGN_IN")
                .setAmount(5L)
                .setBeforeBalance(5L)
                .setAfterBalance(10L)
                .setStatus("SUCCESS")
                .setIdempotencyKey("add-1");
        when(queryMapper.selectAccountForUpdate("u1")).thenReturn(account);
        when(queryMapper.selectByIdempotencyKey("u1", "add-1")).thenReturn(existing);

        TsPointsTransactionVo result = service.add(
                change("u1", 5L, "SIGN_IN", "add-1"));

        assertEquals("PTS1", result.getTransactionNo());
        verify(queryMapper, never()).increaseBalance(any(), any());
        verify(transactionMapper, never()).insert(any(TsPointsTransaction.class));
    }

    /** 已完成的重复退款必须返回原结果，不再次执行超额判断。 */
    @Test
    void refundShouldReturnExistingResultBeforeRefundLimitCheck() {
        TsPointsTransaction original = new TsPointsTransaction()
                .setTransactionNo("ORIGINAL")
                .setUserId("u1")
                .setDirection("EXPENSE")
                .setBizType("AI_CHAT")
                .setAmount(10L)
                .setStatus("SUCCESS");
        TsPointsTransaction existingRefund = new TsPointsTransaction()
                .setId(2L)
                .setTransactionNo("REFUND")
                .setUserId("u1")
                .setDirection("INCOME")
                .setBizType("REFUND")
                .setAmount(10L)
                .setStatus("SUCCESS")
                .setIdempotencyKey("refund-1");
        when(queryMapper.selectTransactionForUpdate("u1", "ORIGINAL"))
                .thenReturn(original);
        when(queryMapper.selectByIdempotencyKey("u1", "refund-1"))
                .thenReturn(existingRefund);

        TsPointsRefundDto request = new TsPointsRefundDto();
        request.setUserId("u1");
        request.setOriginalTransactionNo("ORIGINAL");
        request.setAmount(10L);
        request.setReason("失败返还");
        request.setIdempotencyKey("refund-1");

        TsPointsTransactionVo result = service.refund(request);

        assertEquals("REFUND", result.getTransactionNo());
        verify(queryMapper, never()).sumSuccessfulRefunds(any());
    }

    /** 退款缺少幂等键时必须返回业务错误，不进入数据库查询。 */
    @Test
    void refundShouldRejectMissingIdempotencyKey() {
        TsPointsRefundDto request = new TsPointsRefundDto();
        request.setUserId("u1");
        request.setOriginalTransactionNo("ORIGINAL");
        request.setAmount(10L);

        TsPointsBizException exception = assertThrows(
                TsPointsBizException.class,
                () -> service.refund(request));

        assertEquals(TsPointsErrorCode.POINTS_DUPLICATE_REQUEST, exception.getErrorCode());
        verify(queryMapper, never()).selectTransactionForUpdate(any(), any());
    }

    /** 构建积分变化请求。 */
    private TsPointsChangeDto change(
            String userId, long amount, String bizType, String idempotencyKey) {
        TsPointsChangeDto request = new TsPointsChangeDto();
        request.setUserId(userId);
        request.setAmount(amount);
        request.setBizType(bizType);
        request.setIdempotencyKey(idempotencyKey);
        return request;
    }
}
