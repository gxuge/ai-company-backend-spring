package org.jeecg.modules.system.util.tsactivity;

import org.jeecg.modules.system.enums.tsactivity.TsActivityTaskCategory;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.Date;
import java.util.Locale;

/** 活动任务周期键工具。 */
public final class TsActivityCycleUtils {

    private static final ZoneId ACTIVITY_ZONE = ZoneId.of("Asia/Shanghai");

    private TsActivityCycleUtils() {
    }

    /** 获取活动业务日期。 */
    public static LocalDate businessDate(Date now) {
        Date source = now == null ? new Date() : now;
        return source.toInstant().atZone(ACTIVITY_ZONE).toLocalDate();
    }

    /** 按任务周期生成稳定周期键。 */
    public static String cycleKey(String category, Date now) {
        TsActivityTaskCategory taskCategory =
                TsActivityTaskCategory.valueOf(category.toUpperCase(Locale.ROOT));
        LocalDate date = businessDate(now);
        if (taskCategory == TsActivityTaskCategory.DAILY) {
            return date.toString().replace("-", "");
        }
        if (taskCategory == TsActivityTaskCategory.WEEKLY) {
            WeekFields fields = WeekFields.ISO;
            return date.get(fields.weekBasedYear())
                    + "-W"
                    + String.format(Locale.ROOT, "%02d", date.get(fields.weekOfWeekBasedYear()));
        }
        return "LONG";
    }
}
