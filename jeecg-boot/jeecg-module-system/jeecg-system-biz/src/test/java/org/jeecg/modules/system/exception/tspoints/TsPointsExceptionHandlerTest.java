package org.jeecg.modules.system.exception.tspoints;

import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.enums.tspoints.TsPointsErrorCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** 积分机器错误码响应测试。 */
class TsPointsExceptionHandlerTest {

    /** 余额不足必须写入统一响应的 errorCode 字段。 */
    @Test
    void shouldExposePointsNotEnoughAsMachineReadableCode() {
        TsPointsBizException exception = new TsPointsBizException(
                TsPointsErrorCode.POINTS_NOT_ENOUGH,
                "积分余额不足",
                false,
                Map.of("required", 100L, "balance", 60L));

        Result<Void> result = new TsPointsExceptionHandler()
                .handlePointsBizException(exception);

        assertFalse(result.isSuccess());
        assertEquals(409, result.getCode());
        assertEquals("POINTS_NOT_ENOUGH", result.getErrorCode());
        assertEquals("POINTS", result.getErrorCategory());
        assertEquals(60L, result.getErrorArgs().get("balance"));
    }
}
