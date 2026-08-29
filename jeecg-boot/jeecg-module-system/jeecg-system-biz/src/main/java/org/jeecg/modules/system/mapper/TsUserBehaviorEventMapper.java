package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.dynamic.datasource.annotation.DS;
import org.jeecg.modules.system.entity.TsUserBehaviorEvent;

/** ClickHouse 行为事件 Mapper。 */
@DS("clickhouse")
public interface TsUserBehaviorEventMapper extends BaseMapper<TsUserBehaviorEvent> {

    /** 写入单条业务行为事件。 */
    int insertEvent(TsUserBehaviorEvent event);
}
