package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.jeecg.modules.system.entity.TsUserBehaviorEvent;

/** 推荐用户行为事件 Mapper。 */
public interface TsUserBehaviorEventMapper extends BaseMapper<TsUserBehaviorEvent> {

    /** 按事件ID幂等写入行为明细。 */
    int insertIgnore(TsUserBehaviorEvent event);
}
