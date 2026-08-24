package org.jeecg.modules.system.reward;

import org.jeecg.modules.system.entity.TsRewardEvent;
import org.jeecg.modules.system.enums.tsreward.TsRewardErrorCode;
import org.jeecg.modules.system.enums.tsreward.TsRewardEventType;
import org.jeecg.modules.system.exception.tsreward.TsRewardBizException;
import org.jeecg.modules.system.vo.tsreward.TsRewardEventResultVo;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 根据事件类型选择奖励处理器。 */
@Component
public class TsRewardEventDispatcher {

    private final Map<TsRewardEventType, TsRewardEventHandler> handlerMap;

    /** 注册全部奖励事件处理器，并拒绝重复事件类型。 */
    public TsRewardEventDispatcher(List<TsRewardEventHandler> handlers) {
        this.handlerMap = new EnumMap<>(TsRewardEventType.class);
        for (TsRewardEventHandler handler : handlers) {
            for (TsRewardEventType eventType : handler.supportedTypes()) {
                if (handlerMap.putIfAbsent(eventType, handler) != null) {
                    throw new IllegalStateException("奖励事件处理器重复注册: " + eventType);
                }
            }
        }
    }

    /** 分发并执行指定奖励事件。 */
    public TsRewardEventResultVo dispatch(TsRewardEvent event) {
        TsRewardEventType eventType;
        try {
            eventType = TsRewardEventType.valueOf(event.getEventType());
        } catch (IllegalArgumentException exception) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_TYPE_UNSUPPORTED,
                    "不支持的奖励事件类型");
        }
        TsRewardEventHandler handler = handlerMap.get(eventType);
        if (handler == null) {
            throw new TsRewardBizException(
                    TsRewardErrorCode.REWARD_EVENT_TYPE_UNSUPPORTED,
                    "奖励事件未配置处理器");
        }
        return handler.handle(event);
    }
}
