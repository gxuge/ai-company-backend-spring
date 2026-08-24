package org.jeecg.modules.system.util.tsactivity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 活动周期键工具测试。 */
class TsActivityCycleUtilsTest {

    /** 每日任务必须使用上海业务日期。 */
    @Test
    void dailyCycleShouldUseShanghaiDate() {
        Date now = Date.from(LocalDateTime.of(2026, 8, 20, 10, 0)
                .atZone(ZoneId.of("Asia/Shanghai"))
                .toInstant());

        assertEquals("20260820", TsActivityCycleUtils.cycleKey("DAILY", now));
    }

    /** 长期任务必须使用固定周期键。 */
    @Test
    void longTermCycleShouldUseStableKey() {
        assertEquals("LONG", TsActivityCycleUtils.cycleKey("LONG_TERM", new Date()));
    }
}
