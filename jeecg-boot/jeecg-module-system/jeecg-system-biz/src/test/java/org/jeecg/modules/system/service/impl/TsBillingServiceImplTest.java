package org.jeecg.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.jeecg.modules.system.dto.tsbilling.TsBillingQueryDto;
import org.jeecg.modules.system.mapper.TsBillingQueryMapper;
import org.jeecg.modules.system.vo.tsbilling.TsBillingRecordVo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 双视角账单服务测试。 */
class TsBillingServiceImplTest {

    /** 用户账单必须固定用户归属并使用用户视角。 */
    @Test
    void userBillsShouldIgnoreKeywordAndUseUserPerspective() {
        TsBillingQueryMapper mapper = mock(TsBillingQueryMapper.class);
        when(mapper.selectBillingPage(any(), any(), eq("u1"), eq(false)))
                .thenReturn(new Page<>());
        TsBillingServiceImpl service = new TsBillingServiceImpl(mapper);
        TsBillingQueryDto request = new TsBillingQueryDto();
        request.setKeyword("other-user");
        request.setMoneyDirection("expense");

        service.pageUserBills("u1", request);

        ArgumentCaptor<TsBillingQueryDto> captor =
                ArgumentCaptor.forClass(TsBillingQueryDto.class);
        verify(mapper).selectBillingPage(any(), captor.capture(), eq("u1"), eq(false));
        assertNull(captor.getValue().getKeyword());
        assertEquals("EXPENSE", captor.getValue().getMoneyDirection());
    }

    /** 平台账单必须不附带用户归属并使用平台视角。 */
    @Test
    void platformBillsShouldUsePlatformPerspective() {
        TsBillingQueryMapper mapper = mock(TsBillingQueryMapper.class);
        when(mapper.selectBillingPage(any(), any(), isNull(), eq(true)))
                .thenReturn(new Page<TsBillingRecordVo>());
        TsBillingServiceImpl service = new TsBillingServiceImpl(mapper);

        service.pagePlatformBills(new TsBillingQueryDto());

        ArgumentCaptor<Boolean> perspective = ArgumentCaptor.forClass(Boolean.class);
        verify(mapper).selectBillingPage(any(), any(), isNull(), perspective.capture());
        assertTrue(perspective.getValue());
    }
}
