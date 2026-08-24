package org.jeecg.modules.system.reward;

import org.jeecg.modules.system.entity.TsRewardEvent;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventType;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;

import java.util.Set;

/** 单类奖励事件策略处理器。 */
public interface TsRewardEventHandler {

    /** 返回当前处理器支持的事件类型。 */
    Set<TsRewardEventType> supportedTypes();

    /** 按持久化事件负载执行奖励规则。 */
    TsRewardEventResultVo handle(TsRewardEvent event);
}
